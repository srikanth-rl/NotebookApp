package com.zoho.notebook.controller;

import java.io.IOException;
import java.util.logging.Logger;

import com.zoho.notebook.model.UserModel;
import com.zoho.notebook.model.UserSessionModel;
import com.zoho.notebook.util.ResponseMapper;
import com.zoho.notebook.util.RequestValidator;
import com.zoho.notebook.util.FrequentlyUsedMethods;
import com.zoho.notebook.service.UserService;
import com.zoho.notebook.service.UserSessionService;
import com.zoho.notebook.dto.request.UserDtoRequest;
import com.zoho.notebook.dto.response.UserDtoResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UserLoginServlet extends BaseServlet {
	
	private static final long serialVersionUID = 1L;
    private final UserService userService = new UserService();
	private final UserSessionService userSessionService =  new UserSessionService();
	private static final Logger LOGGER = Logger.getLogger(UserLoginServlet.class.getName());
	
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            UserDtoRequest request = FrequentlyUsedMethods.readJson(req, UserDtoRequest.class);
            if (!RequestValidator.validateLogin(request)) {
                LOGGER.warning("Login Blocked: Invalid input data.");
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid login data.");
                return;
            }
            
            UserModel userData = userService.getUserLoginDetails(request);
            if (userData != null) {
                UserSessionModel sessionData = new UserSessionModel();
                sessionData.setIpAddress(req.getRemoteAddr());
                sessionData.setUserAgent(req.getHeader("User-Agent"));

                UserSessionModel createdSession = userSessionService.createSession(sessionData, userData);
                
                if (createdSession != null) {
                    FrequentlyUsedMethods.setSessionCookieValue(resp, createdSession.getSessionId(), createdSession.getExpiresAt());
                    
                    LOGGER.info("Login Successful: User ID [" + userData.getId() + "]");
                    
                    UserDtoResponse response = ResponseMapper.toLoginResponse(userData);
                    sendSuccess(resp, "Login Successful", response);
                } else {
                    LOGGER.severe("Session Failure: Could not create session for User ID [" + userData.getId() + "]");
                    sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Session creation failed.");
                }
            } else {
                LOGGER.warning("Login Failed: Invalid credentials for [" + request.getValue() + "]");
                sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
            }
        } catch (Exception e) {
            sendServerError(resp, e);
        }
    }
}