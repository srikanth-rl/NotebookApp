package com.zoho.notebook.model;

public class UserSessionModel {

    private String sessionId;
    private Long userId;
    private String ipAddress;
    private String userAgent;
    private Long createdAt;
    private Integer expiresAt;

    public UserSessionModel() {}

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Long getCreatedAt() {
    	return createdAt;
    }
    
    public void setCreatedAt(Long createdAt) {
    	this.createdAt = createdAt;
    }
    
    public int getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Integer expiresAt) {
        this.expiresAt = expiresAt;
    }

}