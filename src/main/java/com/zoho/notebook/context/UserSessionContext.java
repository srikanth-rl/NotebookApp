package com.zoho.notebook.context;

import java.util.Map;
import java.util.HashMap;

public class UserSessionContext {
	private static final ThreadLocal<Map<String, Object>> userContext = new ThreadLocal<>();
	
	public static void set(long id, String name, String timeZone, String clientIp) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", id);
        map.put("displayName", name);
        map.put("timeZone", timeZone);
        map.put("clientIp", clientIp);
        userContext.set(map);
    }
	
	public static void remove() {
		userContext.remove(); // clear ThreadLocal
	}

    public static Long getId() {
        return (userContext.get() != null) ? (Long) userContext.get().get("userId") : null;
    }

    public static String getDisplayName() {
        return (userContext.get() != null) ? (String) userContext.get().get("displayName") : null;
    }

    public static String getTimezone() {
        return (userContext.get() != null) ? (String) userContext.get().get("timeZone") : null;
    }
    
    public static String getClientIp() {
    	return (userContext.get() != null) ? (String) userContext.get().get("clientIp") : null;
    }
}