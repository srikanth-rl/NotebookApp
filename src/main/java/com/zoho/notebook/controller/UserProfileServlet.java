package com.zoho.notebook.controller;

import com.adventnet.iam.IAMUtil;
import com.adventnet.iam.User;

import com.zoho.notebook.context.UserSessionContext;
import com.zoho.notebook.model.UserModel;
import com.zoho.notebook.util.ResponseMapper;
import com.zoho.notebook.util.RequestValidator;
import com.zoho.notebook.util.FrequentlyUsedMethods;
import com.zoho.notebook.service.UserService;
import com.zoho.notebook.dto.request.UserDtoRequest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Logger;

public class UserProfileServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private final UserService userService = new UserService();
    private static final Logger LOGGER = Logger.getLogger(UserProfileServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            LOGGER.info("Profile fetch request received");

            User iamUser = IAMUtil.getCurrentUser();

            if (iamUser == null) {
                LOGGER.warning("IAM user not found in threadlocal — not authenticated");
                sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Not authenticated");
                return;
            }

            // Build UserModel from IAM data
            UserModel user = new UserModel();
            user.setId(iamUser.getZUID());
            user.setDisplayName(iamUser.getDisplayName());
            user.setTimezone(iamUser.getTimezone());
            
            UserModel localUser = userService.getUserProfileDetails();
            if (localUser != null) {
                if (FrequentlyUsedMethods.isNotNullAndNotEmpty(localUser.getDisplayName())) {
                    user.setDisplayName(localUser.getDisplayName());
                }
                if (FrequentlyUsedMethods.isNotNullAndNotEmpty(localUser.getTimezone())) {
                    user.setTimezone(localUser.getTimezone());
                }
                user.setStatus(localUser.getStatus());
                user.setCreatedAt(localUser.getCreatedAt());
                user.setUpdatedAt(localUser.getUpdatedAt());
            }

            LOGGER.info("Profile fetched successfully for ZUID: " + iamUser.getZUID());
            sendSuccess(resp, "Profile Fetched", ResponseMapper.toProfileResponse(user));

        } catch (Exception e) {
            LOGGER.severe("Error while fetching profile: " + e.getMessage());
            sendServerError(resp, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
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
                LOGGER.info("User profile updated in database for ZUID: " + UserSessionContext.getId());
                sendSuccess(resp, "Profile updated successfully");
            } else {
                LOGGER.info("Profile update skipped — no changes detected");
                sendSuccess(resp, "No changes detected");
            }

        } catch (Exception e) {
            LOGGER.severe("Error while updating profile: " + e.getMessage());
            sendServerError(resp, e);
        }
    }
}