package com.zoho.notebook.cache;

public class CacheFactory {

    private static SessionCacheStrategy activeCache;

    public static synchronized SessionCacheStrategy getSessionCache() { // synchronized - for multi-thread safe 
        if (activeCache == null) {
            String configuredCacheType = "JVM";

            if ("REDIS".equalsIgnoreCase(configuredCacheType)) {
                throw new UnsupportedOperationException("Redis cache is not yet implemented.");
            } else {
                activeCache = new JvmCacheStrategy();
            }
        }
        return activeCache;
    }
}