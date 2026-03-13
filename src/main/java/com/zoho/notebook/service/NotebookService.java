package com.zoho.notebook.service;

import com.zoho.notebook.model.NotebookModel;
import com.zoho.notebook.enums.OwnerType;
import com.zoho.notebook.dao.NotebookDao;
import com.zoho.notebook.util.DBConnection;
import com.zoho.notebook.cache.LRUCacheMap;
import com.zoho.notebook.context.UserSessionContext;
import com.zoho.notebook.dto.request.NotebookDtoRequest;

import java.util.List;
import java.sql.Connection;
import java.util.logging.Logger;

public class NotebookService {

    private final NotebookDao notebookDao = new NotebookDao();
    private static final LRUCacheMap<Long, NotebookModel> notebookCache = new LRUCacheMap<>(500); // 500 notebooks stored in memory for the user
    private static final Logger LOGGER = Logger.getLogger(NotebookService.class.getName());
    
    public boolean createNotebook(NotebookDtoRequest req) throws Exception {
        Connection con = null;
        boolean created = false;
        
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);
            
            NotebookModel nb = new NotebookModel();
            Long userId = UserSessionContext.getId();
            
            nb.setTitle(req.getTitle());
            nb.setOwnerType(OwnerType.USER.name());
            nb.setOwnerId(userId);
            nb.setCreatedBy(userId);
            nb.setUpdatedBy(userId);

            Long generatedNbId = notebookDao.insert(con, nb);

            if (generatedNbId != null && generatedNbId > 0) {
                con.commit();
                created = true;
                notebookCache.put(generatedNbId, nb); // put an entry in cache memory
                LOGGER.info("Notebook created successfully. notebookId=" + generatedNbId + ", userId=" + userId);
            }
            else {
                con.rollback();
            }
        } catch (Exception e) {
            LOGGER.severe("Notebook creation failed: " + e.getMessage());
            if (con != null) {
                con.rollback();
            }
            throw e;
        } finally {
            if (con != null) {
                con.close();
            }
        }

        return created;
    }
    
    public NotebookModel getNotebookById(long notebookId) throws Exception {
        NotebookModel cachedNb = notebookCache.get(notebookId); // try to get notebook details from cache memory
        if (cachedNb != null) {
            return cachedNb;
        }
        
        try (Connection con = DBConnection.getConnection()) {
            return notebookDao.getNotebookById(con, UserSessionContext.getId(), notebookId);
        }
    }

    public List<NotebookModel> getUserNotebooks() throws Exception {
        try (Connection con = DBConnection.getConnection()) {
            return notebookDao.getAllNotebooksByOwner(con, UserSessionContext.getId(), OwnerType.USER, UserSessionContext.getTimeZone());
        }
    }

    public boolean updateNotebook(long notebookId, NotebookDtoRequest req) throws Exception {
        Connection con = null;
        boolean updated = false;

        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);
            
            String title = req.getTitle();
            Long userId = UserSessionContext.getId();
            NotebookModel nb = notebookDao.getNotebookById(con, userId, notebookId);

            if (nb == null || nb.getOwnerId() != userId) {
                LOGGER.warning("Unauthorized notebook update attempt. notebookId=" + notebookId + ", userId=" + userId);
                return false;
            }

            updated = notebookDao.update(con, notebookId, title, userId);

            if (updated) {
                con.commit();
                // Cache Invalidation: Removing old data so next GET fetches updated Data
                notebookCache.remove(notebookId);
                LOGGER.info("Notebook updated successfully. notebookId=" + notebookId + ", userId=" + userId);
            } else {
                con.rollback();
            }
        } catch (Exception e) {
            LOGGER.severe("Notebook update failed: " + e.getMessage());
            if (con != null) {
                con.rollback();
            }
            throw e;
        } finally {
            if (con != null) {
                con.close();
            }
        }

        return updated;
    }

    public boolean deleteNotebook(long notebookId) throws Exception {
        Connection con = null;
        boolean deleted = false;

        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);
            
            Long userId = UserSessionContext.getId();
            NotebookModel nb = notebookDao.getNotebookById(con, userId, notebookId);

            if (nb == null || nb.getOwnerId() != userId) {
                LOGGER.warning("Unauthorized notebook delete attempt. notebookId=" + notebookId + ", userId=" + userId);
                return false;
            }

            deleted = notebookDao.delete(con, notebookId);

            if (deleted) {
                con.commit();
                // Removing from notebook cache memory permanently
                notebookCache.remove(notebookId);
                LOGGER.info("Notebook deleted successfully. notebookId=" + notebookId + ", userId=" + userId);
            } else {
                con.rollback();
            }
        } catch (Exception e) {
            LOGGER.severe("Notebook delete failed: " + e.getMessage());
            if (con != null) {
                con.rollback();
            }
            throw e;
        } finally {
            if (con != null) {
                con.close();
            }
        }

        return deleted;
    }
}