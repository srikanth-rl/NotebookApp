package com.zoho.notebook.filter;

import com.zoho.notebook.model.UserModel;
import com.zoho.notebook.context.UserSessionContext;
import com.zoho.notebook.service.UserSessionService;
import com.zoho.notebook.util.FrequentlyUsedMethods;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Map;
import java.util.Set;

import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

public class AuthenticationFilter implements Filter {

    private static Set<String> AUTH_ENDPOINTS = new HashSet<>();
    private static Set<String> PUBLIC_ENDPOINTS = new HashSet<>();
    private static final Logger LOGGER = Logger.getLogger(AuthenticationFilter.class.getName());

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        loadConfig(); // calls only once after server start up
    }

    @SuppressWarnings("unchecked")
	private void loadConfig() throws ServletException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("endpoints.yaml")) {
            if (input == null) {
                LOGGER.severe("endpoints.yaml not found");
                throw new FileNotFoundException("endpoints.yaml not found");
            }

            Yaml yaml = new Yaml();
            // Load the YAML into a Map structure
            Map<String, Object> data = yaml.load(input);
            Map<String, List<String>> endpoints = (Map<String, List<String>>) data.get("endpoints");
            
            // Populate and add in Sets for O(1) lookup during filtering
            AUTH_ENDPOINTS = new HashSet<>(endpoints.getOrDefault("auth_endpoints", List.of()));
            PUBLIC_ENDPOINTS = new HashSet<>(endpoints.getOrDefault("public_endpoints", List.of()));
            
            LOGGER.info("Endpoints loaded successfully");
            System.out.println("\nEndpoints loaded successfully!\n");
        } catch (Exception e) {
            LOGGER.severe("Failed to load endpoints from YAML: " + e.getMessage());
        	e.printStackTrace();
            throw new ServletException("Failed to load endpoints from YAML", e);
        }
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        
        String requestURI = req.getRequestURI();
        LOGGER.info("Incoming request: " + requestURI);

        // Allow public endpoints (CSS, JS, Index) to pass through immediately!
        if (PUBLIC_ENDPOINTS.stream().anyMatch(requestURI::contains)) {
            LOGGER.info("Public endpoint accessed: " + requestURI);
            chain.doFilter(request, response);
            return;
        }
        
        // Extract Session ID from cookie if exists
        String sessionId = FrequentlyUsedMethods.getSessionCookieValue(req);
        UserModel sessionUser = null;
        
        // If custom sessionId exists in cookie, validate it
        if (sessionId != null) {
            try {
                LOGGER.info("Validating session for sessionId");

                // Validate the user is eligible to use the session using DB/cache
                UserSessionService sessionService = new UserSessionService();
                sessionUser = sessionService.validateSessionAndGetUser(sessionId);
            } catch (Exception e) {
                LOGGER.severe("Session validation failed: " + e.getMessage());
                e.printStackTrace();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
        }
        
        Map<String, String> responseMap = new HashMap<>();
        
        boolean isAuthPage = AUTH_ENDPOINTS.stream().anyMatch(requestURI::endsWith);

        // If user has valid session no need to login again
        if (sessionUser != null) {
            LOGGER.info("Valid session detected");

            // but if they try to access login/signup while already logged in
            if (isAuthPage) {
                LOGGER.info("User already logged in attempted auth endpoint");

            	resp.setStatus(HttpServletResponse.SC_OK);
                responseMap.put("status", "success");
                responseMap.put("displayName", sessionUser.getDisplayName());
                responseMap.put("isAlreadyLoggedIn", "true");
                responseMap.put("message", "User is already loggedIn!");

                FrequentlyUsedMethods.writeJson(resp, responseMap);
                return;
            }

            // if any other page they want to access, allow them by setting their basic fields in a thread
            try {
                LOGGER.info("Authenticated request allowed");

                UserSessionContext.set(
                    sessionUser.getId(),
                    sessionUser.getDisplayName(),
                    sessionUser.getTimeZone(),
                    req.getRemoteAddr()
                );
                chain.doFilter(request, response);
                return;
            } finally {
            	// clear the details in the thread once the request is finished
                UserSessionContext.remove();
                LOGGER.info("User session context cleared");
            }
        }

        // If the new User is NOT logged in, and try to access login/signup, allow
        if (isAuthPage) {
            chain.doFilter(req, response);
            return;
        }

        // if any other page they want to access, without logged in, they are not authorized
        LOGGER.warning("Unauthorized access attempt: " + requestURI);

        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        responseMap.put("status", "error");
        responseMap.put("message", "User session expired. Please login again.");
        FrequentlyUsedMethods.writeJson(resp, responseMap);
        return;
    }
}