package com.zoho.notebook.controller;

import java.io.IOException;
import java.util.logging.Logger;

import com.zoho.notebook.service.UserSessionService;
import com.zoho.notebook.util.FrequentlyUsedMethods;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UserLogoutServlet extends BaseServlet {
	
	private static final long serialVersionUID = 1L;
	private final UserSessionService userSessionService =  new UserSessionService();
	private static final Logger LOGGER = Logger.getLogger(UserLogoutServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    	try {
            String sessionId = FrequentlyUsedMethods.getSessionCookieValue(req);
            if (sessionId != null) {
                
                // if sessionId deleted from both Database and the Cache
                if (userSessionService.deleteSession(sessionId)) { 
                    
                    // Reset an empty Cookie value in existing session and (set timeout to 0)
                    FrequentlyUsedMethods.setSessionCookieValue(resp, "", 0);
                }
            }
            
            LOGGER.info("Logged out successfully");
            sendSuccess(resp, "Logged out successfully");
            
        } catch (Exception e) {
            sendServerError(resp, e);
        }
    }
}