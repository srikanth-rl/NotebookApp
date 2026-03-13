package com.zoho.notebook.controller;

import java.io.IOException;
import java.util.logging.Logger;

import com.zoho.notebook.util.RequestValidator;
import com.zoho.notebook.util.FrequentlyUsedMethods;
import com.zoho.notebook.service.UserService;
import com.zoho.notebook.dto.request.UserDtoRequest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UserSignupServlet extends BaseServlet {
	
	private static final long serialVersionUID = 1L;
    private final UserService userService = new UserService();
	private static final Logger LOGGER = Logger.getLogger(UserSignupServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            UserDtoRequest request = FrequentlyUsedMethods.readJson(req, UserDtoRequest.class);
            if (!RequestValidator.validateSignup(request)) {
                LOGGER.warning("Signup Blocked: Validation failed.");
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid signup data.");
                return;
            }

            if (userService.register(request)) {
                LOGGER.info("Signup Successful: New user registered [" + request.getEmail() + "]");
                sendSuccess(resp, "Signup Successful");
            } else {
                LOGGER.warning("Signup Failed: Registration failed for [" + request.getEmail() + "]");
                sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
            }
        } catch (Exception e) {
            sendServerError(resp, e);
        }
    }
}