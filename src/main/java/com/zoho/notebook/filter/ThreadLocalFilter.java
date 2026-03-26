package com.zoho.notebook.filter;

import com.zoho.notebook.model.UserModel;
import com.zoho.notebook.context.UserSessionContext;
import com.zoho.notebook.service.UserSessionService;
import com.zoho.notebook.util.FrequentlyUsedMethods;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.logging.Logger;

public class ThreadLocalFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(ThreadLocalFilter.class.getName());
    private final UserSessionService sessionService = new UserSessionService();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String uri = req.getRequestURI();
        UserModel sessionUser = null;

        String sessionId = FrequentlyUsedMethods.getSessionCookieValue(req);
        if (sessionId != null) {
            try {
                sessionUser = sessionService.getSessionUser(sessionId);
                if (sessionUser == null) {
                    sessionUser = sessionService.validateSessionAndGetUser(sessionId);
                }
            } catch (Exception e) {
                LOGGER.warning("ThreadLocalFilter: session lookup failed for URI: " + uri);
            }
        }

        if (sessionUser != null) {
            try {
                UserSessionContext.set(
                    sessionUser.getId(),
                    sessionUser.getDisplayName(),
                    sessionUser.getTimeZone(),
                    req.getRemoteAddr()
                );
                LOGGER.info("ThreadLocal Created for User [" + sessionUser.getDisplayName() + "] for URI: " + uri);
                chain.doFilter(request, response);
            } finally {
                UserSessionContext.remove();
                LOGGER.info("ThreadLocal context cleared for URI: " + uri);
            }
        } else {
            LOGGER.fine("ThreadLocal BYPASS: No authenticated user found for URI: " + uri);
            chain.doFilter(request, response);
        }
    }
}