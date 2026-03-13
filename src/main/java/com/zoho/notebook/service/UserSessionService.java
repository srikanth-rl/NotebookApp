package com.zoho.notebook.service;

import com.zoho.notebook.util.DBConnection;
import com.zoho.notebook.dao.UserSessionDao;
import com.zoho.notebook.model.UserModel;
import com.zoho.notebook.model.UserSessionModel;
import com.zoho.notebook.cache.CacheFactory;
import com.zoho.notebook.cache.SessionCacheStrategy;
import com.zoho.notebook.constants.SystemConfigConstants;

import java.util.UUID;
import java.sql.Connection;
import java.util.logging.Logger;

public class UserSessionService {

    private final UserSessionDao sessionDao = new UserSessionDao();
    private final SystemConfigService configService = new SystemConfigService();
    private static final SessionCacheStrategy sessionCache = CacheFactory.getSessionCache();
    private static final Logger LOGGER = Logger.getLogger(UserSessionService.class.getName());

    public UserSessionModel createSession(UserSessionModel sessionData, UserModel userData) throws Exception {
        Connection con = null;
        boolean created = false;
        String sessionId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            int timeoutMins = (int) configService.getInt(
                    SystemConfigConstants.SESSION_TIMEOUT_MINUTES_K,
                    SystemConfigConstants.DEFAULT_SESSION_TIMEOUT_V
            );
            
            // Set userData in sessionModel to store in DB
            sessionData.setUserId(userData.getId());
            sessionData.setSessionId(sessionId);
            sessionData.setCreatedAt(now);
            sessionData.setExpiresAt(timeoutMins);

            created = sessionDao.insertSessionInDb(con, sessionData); // put sessionData entry in DB

            if (created) {
                con.commit();
                sessionCache.put(sessionId, userData); // put the created session with user model in cache
                LOGGER.info("Session created successfully. userId=" + userData.getId());
                return sessionData;
            } else {
                con.rollback();
                return null;
            }

        } catch (Exception e) {
            LOGGER.severe("Session creation failed: " + e.getMessage());
            if (con != null) 
                con.rollback();
            throw e;
        } finally {
            if (con != null) 
                con.close();
        }
    }

    public UserModel getSessionUser(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) 
            return null;
        return sessionCache.get(sessionId);
    }

    public void updateSessionUser(String sessionId, UserModel updatedUserData) {
        if (sessionId != null && !sessionId.isEmpty() && updatedUserData != null) {
            sessionCache.put(sessionId, updatedUserData);
            LOGGER.info("Session cache updated.");
        }
    }

    public UserModel validateSessionAndGetUser(String sessionId) throws Exception {
        Connection con = null;
        UserModel user = null;
        
        try {
            // get generic configured cache
            user = sessionCache.get(sessionId);
        } catch (Exception e) {
            LOGGER.severe("Session cache read failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        // if any error occurs in cache, continue the main execution using DB approach
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            if (user == null) {
                user = sessionDao.validateSessionAndGetUserFromDb(con, sessionId);
                if (user == null) {
                    return null;
                }
            }

            int timeoutMins = (int) configService.getInt(
                    SystemConfigConstants.SESSION_TIMEOUT_MINUTES_K,
                    SystemConfigConstants.DEFAULT_SESSION_TIMEOUT_V
            );

            boolean updated = sessionDao.updateExpiryTimeInDb(con, sessionId, timeoutMins);

            if (updated) {
                con.commit();
                sessionCache.put(sessionId, user);
            } else {
                con.rollback();
                sessionCache.remove(sessionId);
                user = null;
            }

        } catch (Exception e) {
            LOGGER.severe("Session validation failed: " + e.getMessage());
            if (con != null) con.rollback();
            throw e;
        } finally {
            if (con != null) con.close();
        }

        return user;
    }

    public boolean deleteSession(String sessionId) throws Exception {
        if (sessionId == null || sessionId.isEmpty())
            return false;

        Connection con = null;
        boolean deleted = false;

        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            deleted = sessionDao.deleteSessionFromDb(con, sessionId);

            if (deleted) {
                con.commit();
                sessionCache.remove(sessionId);
                LOGGER.info("Session deleted successfully.");
            } else {
                con.rollback();
            }

        } catch (Exception e) {
            LOGGER.severe("Session deletion failed: " + e.getMessage());
            if (con != null) 
                con.rollback();
            throw e;
        } finally {
            if (con != null) 
                con.close();
        }

        return deleted;
    }
}