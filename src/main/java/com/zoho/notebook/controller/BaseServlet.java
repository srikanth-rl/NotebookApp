package com.zoho.notebook.controller;

import com.zoho.notebook.dto.response.ApiResponse;
import com.zoho.notebook.util.FrequentlyUsedMethods;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BaseServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	protected final Logger LOGGER = Logger.getLogger(this.getClass().getName());
	
    protected void sendSuccess(HttpServletResponse resp, String message) throws IOException {
        FrequentlyUsedMethods.writeJson(resp, ApiResponse.success(message));
    }

    protected void sendSuccess(HttpServletResponse resp, String message, Object data) throws IOException {
        ApiResponse response = new ApiResponse();
        response.setStatus("success");
        response.setMessage(message);
        response.setData(data);
        FrequentlyUsedMethods.writeJson(resp, response);
    }

    protected void sendError(HttpServletResponse resp, int statusCode, String message) throws IOException {
        resp.setStatus(statusCode);
        FrequentlyUsedMethods.writeJson(resp, ApiResponse.error(message));
    }

    protected void sendServerError(HttpServletResponse resp, Exception e) throws IOException {
    	LOGGER.log(Level.SEVERE, "Internal Server Error occurred", e);
    	String msg = e.getMessage() != null ? e.getMessage().replace("\"", "'") : "Unknown Error";
        sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
    }
}
