package com.zoho.notebook.filter;

import com.zoho.notebook.model.UserModel;
import com.zoho.notebook.util.FrequentlyUsedMethods;
import com.zoho.notebook.service.UserSessionService;
import com.adventnet.iam.security.ZSecAuthenticationProviderImpl; // security Framework Auth provider

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.yaml.snakeyaml.Yaml;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.HashSet;
import java.util.logging.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileNotFoundException;

public class CustomAuthenticationProvider extends ZSecAuthenticationProviderImpl {

    private static Set<String> AUTH_ENDPOINTS = new HashSet<>();
    private static Set<String> PUBLIC_ENDPOINTS = new HashSet<>();
    private final UserSessionService sessionService = new UserSessionService();
    private static final Logger LOGGER = Logger.getLogger(CustomAuthenticationProvider.class.getName());

    @Override
    @SuppressWarnings("unchecked")
    public void init(FilterConfig filterConfig) throws ServletException {
        // Load YAML configuration once at startup
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("endpoints.yaml")) {
            if (input == null) { throw new FileNotFoundException("endpoints.yaml not found"); }

            Map<String, Object> data = new Yaml().load(input);
			Map<String, List<String>> endpoints = (Map<String, List<String>>) data.get("endpoints");
            
            AUTH_ENDPOINTS = new HashSet<>(endpoints.getOrDefault("auth_endpoints", List.of()));
            PUBLIC_ENDPOINTS = new HashSet<>(endpoints.getOrDefault("public_endpoints", List.of()));
            
            LOGGER.info("Security endpoints loaded successfully from YAML.");
        } catch (Exception e) {
            throw new ServletException("Failed to initialize CustomAuthenticationProvider", e);
        }
    }

    @Override
    public boolean isAuthenticated(HttpServletRequest req, HttpServletResponse res) 
            throws ServletException, IOException {

        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        
        // Allow Static Assets/Public endpoints immediately
        if (PUBLIC_ENDPOINTS.stream().anyMatch(uri::equals) || uri.equals(contextPath + "/")) {
            return true; 
        }

        // Validate Session
        String sessionId = FrequentlyUsedMethods.getSessionCookieValue(req);
        UserModel sessionUser = null;

        if (sessionId != null) {
            try {
                sessionUser = sessionService.validateSessionAndGetUser(sessionId);
            } catch (Exception e) {
                LOGGER.severe("Session validation failed: " + e.getMessage()); // No I18N
                return false; 
            }
        }

        boolean isAuthPage = AUTH_ENDPOINTS.stream().anyMatch(uri::endsWith);

        // if sessionUser exists
        if (sessionUser != null) {
            if (isAuthPage) {
                LOGGER.warning("User " + sessionUser.getDisplayName() +" already logged in");
                return false; 
            }

            // Set the userData as an attribute so ThreadLocalFilter can pick it up
            req.setAttribute("authenticatedUser", sessionUser);
            return true;
        }

        // User is a Guest (Not logged in)
        // Allow only if they are hitting the Login/Signup pages (Auth endpoints)
        return isAuthPage; 
    }
}