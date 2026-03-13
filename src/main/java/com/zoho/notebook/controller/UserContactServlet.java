package com.zoho.notebook.controller;

import com.zoho.notebook.model.UserEmailModel;
import com.zoho.notebook.model.UserPhoneModel;
import com.zoho.notebook.service.UserService;
import com.zoho.notebook.util.ResponseMapper;
import com.zoho.notebook.util.RequestValidator;
import com.zoho.notebook.util.FrequentlyUsedMethods;
import com.zoho.notebook.constants.CountryCodesConstants;
import com.zoho.notebook.dto.request.UserDtoRequest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.List;
import java.io.IOException;
import java.util.logging.Logger;

public class UserContactServlet extends BaseServlet {
	
	private static final long serialVersionUID = 1L;
    private final UserService userService = new UserService();
    private static final Logger LOGGER = Logger.getLogger(UserContactServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            LOGGER.info("Fetching user contacts");

            List<UserEmailModel> emails = userService.getEmails();
            List<UserPhoneModel> phones = userService.getPhones();
            
            sendSuccess(resp, "Contacts Fetched", ResponseMapper.toContactResponse(emails, phones));
            LOGGER.info("Contacts fetched successfully");
        } catch (Exception e) {
            LOGGER.severe("Error while fetching contacts: " + e.getMessage());
            sendServerError(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            LOGGER.info("Add contact request received");

        	UserDtoRequest request = FrequentlyUsedMethods.readJson(req, UserDtoRequest.class);
        	
        	if (!RequestValidator.validateContact(request)) {
                LOGGER.warning("Invalid contact data provided for add operation");
        		sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid contact data.");
        		return;
        	}
        	
        	String value = request.getValue();
            boolean added = false;
            
            if (FrequentlyUsedMethods.isValidEmail(value)) {
                LOGGER.info("Adding secondary email");
                added = userService.addSecondaryEmail(value);
            } else if (FrequentlyUsedMethods.isValidPhone(value)) {
                LOGGER.info("Adding secondary phone");

            	String countryCode = request.getCountryCode();
				
				if (!CountryCodesConstants.isValid(countryCode)) {
                    LOGGER.warning("Invalid country code provided");
					throw new Exception("Invalid Country Code provided.");
				}

				String dbPhone = countryCode + "|" + value;
                added = userService.addSecondaryPhone(dbPhone);
            } else {
                LOGGER.warning("Invalid contact type provided");
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid contact type");
                return;
            }

            if (added) {
                LOGGER.info("Contact added successfully");
            	sendSuccess(resp, "Contact added successfully");
            } else {
                LOGGER.warning("Contact already exists");
            	sendError(resp, HttpServletResponse.SC_CONFLICT, value + " already exists");
            }
        } catch (Exception e) {
            LOGGER.severe("Error while adding contact: " + e.getMessage());
            sendServerError(resp, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            LOGGER.info("Update primary contact request received");

        	UserDtoRequest request = FrequentlyUsedMethods.readJson(req, UserDtoRequest.class);
        	
        	if (!RequestValidator.validateContact(request)) {
                LOGGER.warning("Invalid contact data provided for update operation");
        		sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid Contact data.");
        		return;
        	}
        	
        	String value = request.getValue();
            boolean updated = false;
            
            if (FrequentlyUsedMethods.isValidEmail(value)) {
                LOGGER.info("Updating primary email");
            	updated = userService.setPrimaryEmail(value);
            } else if (FrequentlyUsedMethods.isValidPhone(value)) {
                LOGGER.info("Updating primary phone");

            	String countryCode = request.getCountryCode();
				
				if (!CountryCodesConstants.isValid(countryCode)) {
                    LOGGER.warning("Invalid country code provided");
					throw new Exception("Invalid Country Code provided.");
				}

				String dbPhone = countryCode + "|" + value;
            	updated = userService.setPrimaryPhone(dbPhone);
            } else {
                LOGGER.warning("Invalid contact type provided");
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid contact Type");
                return;
            }

            if (updated) {
                LOGGER.info("Primary contact updated successfully");
            	sendSuccess(resp, "Primary contact updated");
            } else {
                LOGGER.warning("Primary contact update failed");
            	sendError(resp, HttpServletResponse.SC_CONFLICT, "Cannot update primary contact.");
            }
        } catch (Exception e) {
            LOGGER.severe("Error while updating contact: " + e.getMessage());
            sendServerError(resp, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            LOGGER.info("Delete contact request received");

        	UserDtoRequest request = FrequentlyUsedMethods.readJson(req, UserDtoRequest.class);
        	
        	if (!RequestValidator.validateContact(request)) {
                LOGGER.warning("Invalid contact data provided for delete operation");
        		sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid Contact data.");
        		return;
        	}
        	
        	String value = request.getValue();
            boolean deleted = false;

            if (FrequentlyUsedMethods.isValidEmail(value)) {
                LOGGER.info("Deleting secondary email");
            	deleted = userService.deleteSecondaryEmail(value);
            } else if (FrequentlyUsedMethods.isValidPhone(value)) {
                LOGGER.info("Deleting secondary phone");

            	String countryCode = request.getCountryCode();
				
				if (!CountryCodesConstants.isValid(countryCode)) {
                    LOGGER.warning("Invalid country code provided");
					throw new Exception("Invalid Country Code provided.");
				}

				String dbPhone = countryCode + "|" + value;
            	deleted = userService.deleteSecondaryPhone(dbPhone);
            } else {
                LOGGER.warning("Invalid contact type provided");
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid contact Type");
                return;
            }

            if (deleted) {
                LOGGER.info("Contact deleted successfully");
            	sendSuccess(resp, "Contact deleted successfully");
            } else {
                LOGGER.warning("Attempt to delete primary contact blocked");
            	sendError(resp, HttpServletResponse.SC_CONFLICT, "Cannot delete primary contact");
            }
        } catch (Exception e) {
            LOGGER.severe("Error while deleting contact: " + e.getMessage());
            sendServerError(resp, e);
        }
    }
}