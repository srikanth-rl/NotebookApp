package com.zoho.notebook.dto.response;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDtoResponse {

    private String displayName;
    private String email;

    private String status;
    private String timeZone;
    private String createdAt;
    private String updatedAt;

    private List<ContactDetail> emails;
    private List<ContactDetail> phones;

    public UserDtoResponse() {}

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
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
    
    public List<ContactDetail> getEmails() {
        return emails;
    }

    public void setEmails(List<ContactDetail> emails) {
        this.emails = emails;
    }

    public List<ContactDetail> getPhones() {
        return phones;
    }

    public void setPhones(List<ContactDetail> phones) {
        this.phones = phones;
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ContactDetail {

        private String value;
        private String countryCode;
        private boolean isPrimary;

        public ContactDetail(String value, String countryCode, boolean isPrimary) {
            this.value = value;
            this.countryCode = countryCode;
            this.isPrimary = isPrimary;
        }

        public String getValue() {
            return value;
        }

        public boolean isPrimary() {
            return isPrimary;
        }

		public String getCountryCode() {
			return countryCode;
		}
    }
    
}