package com.zoho.notebook.util;

import com.zoho.notebook.dto.request.NoteDtoRequest;
import com.zoho.notebook.dto.request.NotebookDtoRequest;
import com.zoho.notebook.dto.request.UserDtoRequest;

public class RequestValidator {

    private RequestValidator() {}

    public static boolean validateSignup(UserDtoRequest req) {
        if (req == null) 
        	return false;
        return FrequentlyUsedMethods.isNotNullAndNotEmpty(req.getDisplayName())
            && FrequentlyUsedMethods.isNotNullAndNotEmpty(req.getEmail())
            && FrequentlyUsedMethods.isNotNullAndNotEmpty(req.getPassword())
            && FrequentlyUsedMethods.isNotNullAndNotEmpty(req.getTimezone());
    }

    public static boolean validateLogin(UserDtoRequest req) {
        if (req == null) 
        	return false;
        return FrequentlyUsedMethods.isNotNullAndNotEmpty(req.getValue()) 
            && FrequentlyUsedMethods.isNotNullAndNotEmpty(req.getPassword());
    }

    public static boolean validateContact(UserDtoRequest req) {
        if (req == null) 
        	return false;
        return FrequentlyUsedMethods.isNotNullAndNotEmpty(req.getValue());
    }

    public static boolean validateUpdateProfile(UserDtoRequest req) {
        if (req == null)
        	return false;
        return FrequentlyUsedMethods.isNotNullAndNotEmpty(req.getDisplayName())
            || FrequentlyUsedMethods.isNotNullAndNotEmpty(req.getTimezone());
    }
    
        
    public static boolean validateNotebook(NotebookDtoRequest req) {
        if (req == null) 
        	return false;
        return FrequentlyUsedMethods.isNotNullAndNotEmpty(req.getTitle());
    }
    
    public static boolean validateNote(NoteDtoRequest req) {
        if (req == null)
            return false;
        return FrequentlyUsedMethods.isNotNullAndNotEmpty(req.getTitle())
            || FrequentlyUsedMethods.isNotNullAndNotEmpty(req.getContent());
    }

    public static boolean validateUpdateNote(NoteDtoRequest req) {
        if (req == null)
            return false;
        return FrequentlyUsedMethods.isNotNullAndNotEmpty(req.getTitle())
            || FrequentlyUsedMethods.isNotNullAndNotEmpty(req.getContent())
            || req.getIsPinned() != null;
    }
}