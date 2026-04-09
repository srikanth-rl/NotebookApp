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
//	private final UserSessionService userSessionService =  new UserSessionService();
	private static final Logger LOGGER = Logger.getLogger(UserLogoutServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    	try {
	    	String serviceName = System.getProperty("service.name");
	    	String iamLogoutUrl = com.adventnet.iam.IAMUtil.getLogoutURL(serviceName, "/");
	        req.getSession().invalidate();
	            
	        LOGGER.info("Logged out successfully");
	        resp.sendRedirect(iamLogoutUrl);
            
        } catch (Exception e) {
            sendServerError(resp, e);
        }
    }
}