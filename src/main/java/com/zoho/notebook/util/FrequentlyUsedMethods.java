package com.zoho.notebook.util;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FrequentlyUsedMethods {

	private static final ObjectMapper MAPPER = new ObjectMapper()
	        											.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public static <T> T readJson(HttpServletRequest req, Class<T> classObject) throws IOException {
        return MAPPER.readValue(req.getReader(), classObject);
    }

    public static void writeJson(HttpServletResponse resp, Object object) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        MAPPER.writeValue(resp.getWriter(), object);
    }
    
    public static String getSessionCookieValue(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("J_SESSION_ID")) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
    
    public static void setSessionCookieValue(HttpServletResponse resp, String newSessionId, int timeoutMinutes) {
        Cookie cookie = new Cookie("J_SESSION_ID", newSessionId);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(timeoutMinutes * 60); // in seconds
        resp.addCookie(cookie);
    }
    
    public static String formatMsToTimezone(Long ms, String userTimezone) {
        if (ms == null) 
        	return null;
        try {
            return Instant.ofEpochMilli(ms)
		                    .atZone(ZoneId.of(userTimezone))
		                    .format(DATE_FORMATTER);
	        } catch (Exception e) {
            return Instant.ofEpochMilli(ms).toString(); // Fallback to UTC time in ISO format
        }
    }
    
	public static boolean isNotNullAndNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
    
    public static Long getPathId(HttpServletRequest req) {
        String pathInfo = req.getPathInfo(); 
        if (pathInfo == null || pathInfo.equals("/")) {
            return null;
        }
        
        try {
            return Long.parseLong(pathInfo.substring(1));
        } catch (Exception e) {
            return null; 
        }
    }

    public static boolean isValidEmail(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        int atIndex = value.indexOf('@');
        int dotIndex = value.lastIndexOf('.');

        return atIndex > 0                         // '@' exists and is not the first character
            && dotIndex > atIndex + 1              // '.' exists, comes after '@', and isn't immediately after it
            && dotIndex < value.length() - 1       // '.' is not the last character
            && !value.contains(" ");               // No spaces allowed
    }
    
    public static boolean isValidPhone(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        // 1. Check Length (10-15 digits)
        if (value.length() < 10 || value.length() > 15) {
            return false;
        }

        // 2. Check Characters
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            // Allow '+' only at the very beginning
            if (i == 0 && c == '+') {
                continue;
            }

            // All other characters must be digits
            if (!Character.isDigit(c)) {
                return false;
            }
        }

        return true;
    }
}