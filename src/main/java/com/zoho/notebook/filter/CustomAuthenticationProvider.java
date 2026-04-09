package com.zoho.notebook.filter;

import com.adventnet.iam.security.ZSecAuthenticationProviderImpl;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Logger;

public class CustomAuthenticationProvider extends ZSecAuthenticationProviderImpl {

    private static final Logger LOGGER = Logger.getLogger(CustomAuthenticationProvider.class.getName());

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        LOGGER.info("CustomAuthenticationProvider initialized!");
    }

    @Override
    public boolean isAuthenticated(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        // Parent class validates the IAM ticket (IAMAGENTTICKET cookie).
        boolean authenticated = super.isAuthenticated(req, res);

        if (!authenticated) {
            String uri = req.getRequestURI();
            LOGGER.warning("Unauthenticated request to: " + uri + " — redirecting to login.jsp");
            res.sendRedirect(req.getContextPath() + "/login.jsp");
            return false;
        }

        return true;
    }
}