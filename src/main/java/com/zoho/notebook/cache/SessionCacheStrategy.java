package com.zoho.notebook.cache;

import com.zoho.notebook.model.UserModel;

public interface SessionCacheStrategy {

    UserModel get(String sessionId);
    void put(String sessionId, UserModel user);
    void remove(String sessionId);
}