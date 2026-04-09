package com.zoho.notebook.controller;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * UserLoginServlet — DEPRECATED.
 * IAM handles authentication via IAMAGENTTICKET cookie.
 * This servlet now redirects to login.jsp which sends the user to IAM sign-in.
 */
public class UserLoginServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = Logger.getLogger(UserLoginServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOGGER.info("Login handled by IAM — redirecting to login.jsp");
        resp.sendRedirect(req.getContextPath() + "/login.jsp");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOGGER.info("Login handled by IAM — redirecting to login.jsp");
        resp.sendRedirect(req.getContextPath() + "/login.jsp");
    }
}