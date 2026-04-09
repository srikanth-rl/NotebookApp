package com.zoho.notebook.model;

public class UserModel {

    private Long id;           // ZUID from IAM
    private String displayName;
    private String status;
    private String createdAt;
    private String updatedAt;
    private String timeZone;

    public UserModel() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getTimezone() {
        return timeZone;
    }

    public void setTimezone(String timeZone) {
        this.timeZone = timeZone;
    }
}