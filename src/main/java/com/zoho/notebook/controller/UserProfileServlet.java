package com.zoho.notebook.controller;

import com.zoho.notebook.model.UserModel;
import com.zoho.notebook.util.ResponseMapper;
import com.zoho.notebook.util.RequestValidator;
import com.zoho.notebook.util.FrequentlyUsedMethods;
import com.zoho.notebook.service.UserService;
import com.zoho.notebook.service.UserSessionService;
import com.zoho.notebook.dto.request.UserDtoRequest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Logger;

public class UserProfileServlet extends BaseServlet {
	
	private static final long serialVersionUID = 1L;
    private final UserService userService = new UserService();
    private final UserSessionService userSessionService = new UserSessionService();
    private static final Logger LOGGER = Logger.getLogger(UserProfileServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            LOGGER.info("Profile fetch request received");

            String sessionId = FrequentlyUsedMethods.getSessionCookieValue(req);
            UserModel user = userSessionService.getSessionUser(sessionId);  // get user profile from session cache
            
            if (user == null) {
                LOGGER.info("User not found in session cache. Fetching from DB");

                user = userService.getUserProfileDetails(); // get user data from db if no user object exists in session
                
                if (user == null) { // if still, user data not exists in DB
                    LOGGER.warning("User not found in database");
                    sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "User not found");
                    return;
                }

                LOGGER.info("User profile fetched from DB and updating session cache");
                // put new user object in cache
            	userSessionService.updateSessionUser(sessionId, user); 
            }

            LOGGER.info("Profile fetched successfully");
            sendSuccess(resp, "Profile Fetched", ResponseMapper.toProfileResponse(user));
        } catch (Exception e) {
            LOGGER.severe("Error while fetching profile: " + e.getMessage());
            sendServerError(resp, e);
        }
    }


    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            LOGGER.info("Profile update request received");

            UserDtoRequest request = FrequentlyUsedMethods.readJson(req, UserDtoRequest.class);
            
            if (!RequestValidator.validateUpdateProfile(request)) {
                LOGGER.warning("Profile update blocked due to invalid input data");
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid profile data.");
                return;
            }
            
            boolean updated = userService.updateUserProfileDetails(request);
            
            if (updated) {
                LOGGER.info("User profile updated in database");

            	UserModel updatedUser = userService.getUserProfileDetails();
            	String sessionId = FrequentlyUsedMethods.getSessionCookieValue(req);
            	
                LOGGER.info("Updating user profile in session cache");

                // update the new data of user profile in cache
            	userSessionService.updateSessionUser(sessionId, updatedUser); 
                sendSuccess(resp, "Profile updated successfully");
            } else {
                LOGGER.info("Profile update skipped - no changes detected");
                sendSuccess(resp, "No changes detected");
            }
        } catch (Exception e) {
            LOGGER.severe("Error while updating profile: " + e.getMessage());
            sendServerError(resp, e);
        }
    }
}