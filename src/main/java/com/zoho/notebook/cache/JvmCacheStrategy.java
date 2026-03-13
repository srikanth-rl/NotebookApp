package com.zoho.notebook.cache;

import com.zoho.notebook.model.UserModel;

public class JvmCacheStrategy implements SessionCacheStrategy {

    private final LRUCacheMap<String, UserModel> lruCache = new LRUCacheMap<>(100);

    @Override
    public UserModel get(String sessionId) {
        return lruCache.get(sessionId);
    }

    @Override
    public void put(String sessionId, UserModel user) {
        lruCache.put(sessionId, user);
    }

    @Override
    public void remove(String sessionId) {
        lruCache.remove(sessionId);
    }
}