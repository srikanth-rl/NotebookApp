package com.zoho.notebook.service;

import com.adventnet.persistence.DataAccess;
import javax.transaction.TransactionManager;

import com.zoho.notebook.dao.UserDao;
import com.zoho.notebook.dao.UserEmailDao;
import com.zoho.notebook.dao.UserPhoneDao;
import com.zoho.notebook.dao.UserPasswordDao;
import com.zoho.notebook.enums.UserStatus;
import com.zoho.notebook.model.UserModel;
import com.zoho.notebook.model.UserEmailModel;
import com.zoho.notebook.model.UserPhoneModel;
import com.zoho.notebook.util.FrequentlyUsedMethods;
import com.zoho.notebook.dto.request.UserDtoRequest;
import com.zoho.notebook.context.UserSessionContext;
import com.zoho.notebook.constants.CountryCodesConstants;

import java.util.List;
import java.util.logging.Logger;

public class UserService {

    private final UserDao         userDao         = new UserDao();
    private final UserEmailDao    userEmailDao    = new UserEmailDao();
    private final UserPhoneDao    userPhoneDao    = new UserPhoneDao();
    private final UserPasswordDao userPasswordDao = new UserPasswordDao();

    private static final Logger LOGGER = Logger.getLogger(UserService.class.getName());

    public boolean register(UserDtoRequest request) throws Exception {
        TransactionManager txMgr = DataAccess.getTransactionManager();
        boolean registered = false;

        try {
            txMgr.begin();

            String email        = request.getEmail();
            String pwd          = request.getPassword();
            String phone        = request.getPhone();
            String displayName  = request.getDisplayName();
            String userTimeZone = request.getTimeZone();

            if (!FrequentlyUsedMethods.isValidEmail(email)) {
                throw new Exception("Invalid Email.");
            }

            if (userEmailDao.isEmailTaken(email)) {
                throw new Exception("Email already exists.");
            }

            UserModel user = new UserModel();
            user.setDisplayName(displayName);
            user.setTimeZone(userTimeZone);

            Long userId = userDao.insertUser(user);

            if (userId == null) {
                txMgr.rollback();
                throw new Exception("User registration failed.");
            }

            boolean emailInserted    = userEmailDao.insert(userId, email, 1);
            boolean passwordInserted = userPasswordDao.insert(userId, pwd);

            if (!emailInserted || !passwordInserted) {
                txMgr.rollback();
                throw new Exception("User registration failed.");
            }

            if (phone != null && !phone.isBlank()) {
                if (!FrequentlyUsedMethods.isValidPhone(phone)) {
                    throw new Exception("Invalid Phone.");
                }

                String countryCode = request.getCountryCode();

                if (!CountryCodesConstants.isValid(countryCode)) {
                    txMgr.rollback();
                    throw new Exception("Invalid Country Code provided.");
                }

                String dbPhone = countryCode + "|" + phone;

                if (userPhoneDao.isPrimaryExists(dbPhone)) {
                    txMgr.rollback();
                    throw new Exception("Phone number already registered.");
                }

                boolean phoneInserted  = userPhoneDao.insert(userId, dbPhone, 1);
                boolean statusUpdated  = userDao.updateUserStatus(userId, UserStatus.VERIFIED);

                if (!phoneInserted || !statusUpdated) {
                    txMgr.rollback();
                    throw new Exception("Phone number registration failed.");
                }
            }

            txMgr.commit();
            registered = true;
            LOGGER.info("User registered successfully. userId=" + userId + ", email=" + email);

        } catch (Exception e) {
            LOGGER.severe("User registration failed: " + e.getMessage());
            txMgr.rollback();
            throw e;
        }

        return registered;
    }

    public UserModel getUserLoginDetails(UserDtoRequest request) throws Exception {
        String value = request.getValue();
        String pwd   = request.getPassword();

        if (FrequentlyUsedMethods.isValidEmail(value)) {
            return userDao.loginByEmail(value, pwd);
        } else if (FrequentlyUsedMethods.isValidPhone(value)) {
            String countryCode = request.getCountryCode();

            if (!CountryCodesConstants.isValid(countryCode)) {
                throw new Exception("Invalid Country Code provided.");
            }

            String dbPhone = countryCode + "|" + value;
            return userDao.loginByPhone(dbPhone, pwd);
        } else {
            throw new Exception("Login type is invalid.");
        }
    }

    public UserModel getUserProfileDetails() throws Exception {
        return userDao.getUserFullDetails(UserSessionContext.getId());
    }

    public List<UserEmailModel> getEmails() throws Exception {
        return userEmailDao.getEmailDetailsByUserId(UserSessionContext.getId(), UserSessionContext.getTimeZone());
    }

    public List<UserPhoneModel> getPhones() throws Exception {
        return userPhoneDao.getPhoneDetailsByUserId(UserSessionContext.getId(), UserSessionContext.getTimeZone());
    }

    public boolean updateUserProfileDetails(UserDtoRequest request) throws Exception {
        TransactionManager txMgr = DataAccess.getTransactionManager();
        boolean updated = false;

        try {
            txMgr.begin();

            String newName      = request.getDisplayName();
            String newTimeZone  = request.getTimeZone();

            Long   userId          = UserSessionContext.getId();
            String currentName     = UserSessionContext.getDisplayName();
            String currentTimeZone = UserSessionContext.getTimeZone();

            boolean isNameChanged     = newName     != null && !newName.equals(currentName);
            boolean isTimeZoneChanged = newTimeZone != null && !newTimeZone.equals(currentTimeZone);

            if (isNameChanged || isTimeZoneChanged) {
                String nameToUpdate = newName     != null ? newName     : currentName;
                String zoneToUpdate = newTimeZone != null ? newTimeZone : currentTimeZone;

                updated = userDao.updateUserProfile(userId, nameToUpdate, zoneToUpdate);

                if (updated) {
                    txMgr.commit();
                    LOGGER.info("User profile updated. userId=" + userId);
                } else {
                    txMgr.rollback();
                }
            } else {
                txMgr.rollback();
            }
        } catch (Exception e) {
            LOGGER.severe("User profile update failed: " + e.getMessage());
            txMgr.rollback();
            throw e;
        }

        return updated;
    }

    public boolean addSecondaryEmail(String email) throws Exception {
        TransactionManager txMgr = DataAccess.getTransactionManager();
        boolean added = false;

        try {
            txMgr.begin();

            Long userId = UserSessionContext.getId();

            if (userEmailDao.isEmailTaken(email)) {
                throw new Exception("Email already exists.");
            }

            added = userEmailDao.insert(userId, email, 0);

            if (added) {
                txMgr.commit();
                LOGGER.info("Secondary email added. userId=" + userId + ", email=" + email);
            } else {
                txMgr.rollback();
            }
        } catch (Exception e) {
            LOGGER.severe("Add secondary email failed: " + e.getMessage());
            txMgr.rollback();
            throw e;
        }

        return added;
    }

    public boolean addSecondaryPhone(String phone) throws Exception {
        TransactionManager txMgr = DataAccess.getTransactionManager();
        boolean added = false;

        try {
            txMgr.begin();

            Long userId = UserSessionContext.getId();

            if (userPhoneDao.isPhoneOwnedByUser(userId, phone)) {
                throw new Exception("Phone number already exists.");
            }

            added = userPhoneDao.insert(userId, phone, 0);

            if (added) {
                txMgr.commit();
                LOGGER.info("Secondary phone added. userId=" + userId);
            } else {
                txMgr.rollback();
            }
        } catch (Exception e) {
            LOGGER.severe("Add secondary phone failed: " + e.getMessage());
            txMgr.rollback();
            throw e;
        }

        return added;
    }

    public boolean setPrimaryEmail(String email) throws Exception {
        TransactionManager txMgr = DataAccess.getTransactionManager();
        boolean updated = false;

        try {
            txMgr.begin();

            Long userId = UserSessionContext.getId();

            if (!userEmailDao.isEmailOwnedByUser(userId, email)) {
                throw new Exception("Email not found.");
            }

            boolean reset = userEmailDao.resetPrimary(userId);
            boolean set   = userEmailDao.setPrimary(userId, email);

            if (reset && set) {
                txMgr.commit();
                updated = true;
                LOGGER.info("Primary email updated. userId=" + userId);
            } else {
                txMgr.rollback();
            }
        } catch (Exception e) {
            LOGGER.severe("Set primary email failed: " + e.getMessage());
            txMgr.rollback();
            throw e;
        }

        return updated;
    }

    public boolean setPrimaryPhone(String phone) throws Exception {
        TransactionManager txMgr = DataAccess.getTransactionManager();
        boolean updated = false;

        try {
            txMgr.begin();

            Long userId = UserSessionContext.getId();

            if (!userPhoneDao.isPhoneOwnedByUser(userId, phone)) {
                throw new Exception("Phone number not found.");
            }

            if (userPhoneDao.isPrimaryExists(phone)) {
                throw new Exception("Phone number is already set as primary.");
            }

            boolean reset         = userPhoneDao.resetPrimary(userId);
            boolean set           = userPhoneDao.setPrimary(userId, phone);
            boolean statusUpdated = userDao.updateUserStatus(userId, UserStatus.VERIFIED);

            if (reset && set && statusUpdated) {
                txMgr.commit();
                updated = true;
                LOGGER.info("Primary phone updated. userId=" + userId);
            } else {
                txMgr.rollback();
            }
        } catch (Exception e) {
            LOGGER.severe("Set primary phone failed: " + e.getMessage());
            txMgr.rollback();
            throw e;
        }

        return updated;
    }

    public boolean deleteSecondaryEmail(String email) throws Exception {
        TransactionManager txMgr = DataAccess.getTransactionManager();
        boolean deleted = false;

        try {
            txMgr.begin();

            Long userId = UserSessionContext.getId();

            if (!userEmailDao.isEmailOwnedByUser(userId, email)) {
                throw new Exception("Email not found.");
            }

            deleted = userEmailDao.delete(userId, email);

            if (deleted) {
                txMgr.commit();
                LOGGER.info("Secondary email deleted. userId=" + userId);
            } else {
                txMgr.rollback();
                throw new Exception("Cannot delete Primary Email Address.");
            }
        } catch (Exception e) {
            LOGGER.severe("Delete secondary email failed: " + e.getMessage());
            txMgr.rollback();
            throw e;
        }

        return deleted;
    }

    public boolean deleteSecondaryPhone(String phone) throws Exception {
        TransactionManager txMgr = DataAccess.getTransactionManager();
        boolean deleted = false;

        try {
            txMgr.begin();

            Long userId = UserSessionContext.getId();

            if (!userPhoneDao.isPhoneOwnedByUser(userId, phone)) {
                throw new Exception("Phone number not found.");
            }

            deleted = userPhoneDao.delete(userId, phone);

            if (deleted) {
                txMgr.commit();
                LOGGER.info("Secondary phone deleted. userId=" + userId);
            } else {
                txMgr.rollback();
                throw new Exception("Cannot delete Primary Phone number.");
            }
        } catch (Exception e) {
            LOGGER.severe("Delete secondary phone failed: " + e.getMessage());
            txMgr.rollback();
            throw e;
        }

        return deleted;
    }
}
