package com.zoho.notebook.filter;

import com.zoho.notebook.model.UserModel;
import com.zoho.notebook.context.UserSessionContext;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.logging.Logger;

public class ThreadLocalFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(ThreadLocalFilter.class.getName());

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest req = (HttpServletRequest) request;
        String uri = req.getRequestURI();
        
        // Pick up the userDetails identified by CustomAuthenticationProvider
        UserModel sessionUser = (UserModel) req.getAttribute("authenticatedUser");

        if (sessionUser != null) {
            try {
                // Establish the context
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
            // Guest flow
            LOGGER.fine("ThreadLocal BYPASS: No authenticated user found for URI: " + uri);
            chain.doFilter(request, response);
        }
    }
}