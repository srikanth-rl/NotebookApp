package com.zoho.notebook.service;

import com.zoho.notebook.dao.UserDao;
import com.zoho.notebook.dao.UserEmailDao;
import com.zoho.notebook.dao.UserPhoneDao;
import com.zoho.notebook.dao.UserPasswordDao;
import com.zoho.notebook.enums.UserStatus;
import com.zoho.notebook.model.UserModel;
import com.zoho.notebook.model.UserEmailModel;
import com.zoho.notebook.model.UserPhoneModel;
import com.zoho.notebook.util.DBConnection;
import com.zoho.notebook.util.FrequentlyUsedMethods;
import com.zoho.notebook.dto.request.UserDtoRequest;
import com.zoho.notebook.context.UserSessionContext;
import com.zoho.notebook.constants.CountryCodesConstants;

import java.sql.Connection;
import java.util.List;
import java.util.logging.Logger;

public class UserService {

	private final UserDao userDao = new UserDao();
	private final UserEmailDao userEmailDao = new UserEmailDao();
	private final UserPhoneDao userPhoneDao = new UserPhoneDao();
	private final UserPasswordDao userPasswordDao = new UserPasswordDao();

	private static final Logger LOGGER = Logger.getLogger(UserService.class.getName());

	public boolean register(UserDtoRequest request) throws Exception {
		Connection con = null;
		boolean registered = false;

		try {
			con = DBConnection.getConnection();
			con.setAutoCommit(false);

			String email = request.getEmail();
			String pwd = request.getPassword();
			String phone = request.getPhone();
			String displayName = request.getDisplayName();
			String userTimeZone = request.getTimeZone();

			if (!FrequentlyUsedMethods.isValidEmail(email)) {
				throw new Exception("Invalid Email.");
			}

			if (userEmailDao.isEmailTaken(con, email)) {
				throw new Exception("Email already exists.");
			}

			UserModel user = new UserModel();
			user.setDisplayName(displayName);
			user.setTimeZone(userTimeZone);

			Long userId = userDao.insertUser(con, user);

			if (userId == null) {
				con.rollback();
				throw new Exception("User registration failed.");
			}

			boolean emailInserted = userEmailDao.insert(con, userId, email, 1);
			boolean passwordInserted = userPasswordDao.insert(con, userId, pwd);

			if (!emailInserted || !passwordInserted) {
				con.rollback();
				throw new Exception("User registration failed.");
			}

			if (phone != null && !phone.isBlank()) {
				if (!FrequentlyUsedMethods.isValidPhone(phone)) {
					throw new Exception("Invalid Phone.");
				}

				String countryCode = request.getCountryCode();

				if (!CountryCodesConstants.isValid(countryCode)) {
					con.rollback();
					throw new Exception("Invalid Country Code provided.");
				}

				String dbPhone = countryCode + "|" + phone;

				if (userPhoneDao.isPrimaryExists(con, dbPhone)) {
					con.rollback();
					throw new Exception("Phone number already registered.");
				}

				boolean phoneInserted = userPhoneDao.insert(con, userId, dbPhone, 1);
				boolean statusUpdated = userDao.updateUserStatus(con, userId, UserStatus.VERIFIED);

				if (!phoneInserted || !statusUpdated) {
					con.rollback();
					throw new Exception("Phone number registration failed.");
				}
			}

			con.commit();
			registered = true;
			LOGGER.info("User registered successfully. userId=" + userId + ", email=" + email);

		} catch (Exception e) {
			LOGGER.severe("User registration failed: " + e.getMessage());
			if (con != null) {
				con.rollback();
			}
			throw e;
		} finally {
			if (con != null) {
				con.close();
			}
		}

		return registered;
	}

	public UserModel getUserLoginDetails(UserDtoRequest request) throws Exception {
		try (Connection con = DBConnection.getConnection()) {
			String value = request.getValue();
			String pwd = request.getPassword();

			if (FrequentlyUsedMethods.isValidEmail(value)) {
				return userDao.loginByEmail(con, value, pwd);
			} else if (FrequentlyUsedMethods.isValidPhone(value)) {
				String countryCode = request.getCountryCode();

				if (!CountryCodesConstants.isValid(countryCode)) {
					throw new Exception("Invalid Country Code provided.");
				}

				String dbPhone = countryCode + "|" + value;
				return userDao.loginByPhone(con, dbPhone, pwd);
			} else {
				throw new Exception("Login type is invalid.");
			}
		}
	}

	public UserModel getUserProfileDetails() throws Exception {
		try (Connection con = DBConnection.getConnection()) {
			return userDao.getUserFullDetails(con, UserSessionContext.getId());
		}
	}

	public List<UserEmailModel> getEmails() throws Exception {
		try (Connection con = DBConnection.getConnection()) {
			return userEmailDao.getEmailDetailsByUserId(con, UserSessionContext.getId(), UserSessionContext.getTimeZone());
		}
	}

	public List<UserPhoneModel> getPhones() throws Exception {
		try (Connection con = DBConnection.getConnection()) {
			return userPhoneDao.getPhoneDetailsByUserId(con, UserSessionContext.getId(), UserSessionContext.getTimeZone());
		}
	}

	public boolean updateUserProfileDetails(UserDtoRequest request) throws Exception {
		Connection con = null;
		boolean updated = false;

		try {
			con = DBConnection.getConnection();
			con.setAutoCommit(false);

			String newName = request.getDisplayName();
			String newTimeZone = request.getTimeZone();

			Long userId = UserSessionContext.getId();
			String currentName = UserSessionContext.getDisplayName();
			String currentTimeZone = UserSessionContext.getTimeZone();

			boolean isNameChanged = newName != null && !newName.equals(currentName);
			boolean isTimeZoneChanged = newTimeZone != null && !newTimeZone.equals(currentTimeZone);

			if (isNameChanged || isTimeZoneChanged) {
				String nameToUpdate = newName != null ? newName : currentName;
				String zoneToUpdate = newTimeZone != null ? newTimeZone : currentTimeZone;

				updated = userDao.updateUserProfile(con, userId, nameToUpdate, zoneToUpdate);

				if (updated) {
					con.commit();
					LOGGER.info("User profile updated. userId=" + userId);
				} else {
					con.rollback();
				}
			}
		} catch (Exception e) {
			LOGGER.severe("User profile update failed: " + e.getMessage());
			if (con != null) {
				con.rollback();
			}
			throw e;
		} finally {
			if (con != null) {
				con.close();
			}
		}

		return updated;
	}

	public boolean addSecondaryEmail(String email) throws Exception {
		Connection con = null;
		boolean added = false;

		try {
			con = DBConnection.getConnection();
			con.setAutoCommit(false);

			Long userId = UserSessionContext.getId();

			if (userEmailDao.isEmailTaken(con, email)) {
				throw new Exception("Email already exists.");
			}

			added = userEmailDao.insert(con, userId, email, 0);

			if (added) {
				con.commit();
				LOGGER.info("Secondary email added. userId=" + userId + ", email=" + email);
			} else {
				con.rollback();
			}
		} catch (Exception e) {
			LOGGER.severe("Add secondary email failed: " + e.getMessage());
			if (con != null) {
				con.rollback();
			}
			throw e;
		} finally {
			if (con != null) {
				con.close();
			}
		}

		return added;
	}

	public boolean addSecondaryPhone(String phone) throws Exception {
		Connection con = null;
		boolean added = false;

		try {
			con = DBConnection.getConnection();
			con.setAutoCommit(false);

			Long userId = UserSessionContext.getId();

			if (userPhoneDao.isPhoneOwnedByUser(con, userId, phone)) {
				throw new Exception("Phone number already exists.");
			}

			added = userPhoneDao.insert(con, userId, phone, 0);

			if (added) {
				con.commit();
				LOGGER.info("Secondary phone added. userId=" + userId);
			} else {
				con.rollback();
			}
		} catch (Exception e) {
			LOGGER.severe("Add secondary phone failed: " + e.getMessage());
			if (con != null) {
				con.rollback();
			}
			throw e;
		} finally {
			if (con != null) {
				con.close();
			}
		}

		return added;
	}

	public boolean setPrimaryEmail(String email) throws Exception {
		Connection con = null;
		boolean updated = false;

		try {
			con = DBConnection.getConnection();
			con.setAutoCommit(false);

			Long userId = UserSessionContext.getId();

			if (!userEmailDao.isEmailOwnedByUser(con, userId, email)) {
				throw new Exception("Email not found.");
			}

			boolean reset = userEmailDao.resetPrimary(con, userId);
			boolean set = userEmailDao.setPrimary(con, userId, email);

			if (reset && set) {
				con.commit();
				updated = true;
				LOGGER.info("Primary email updated. userId=" + userId);
			} else {
				con.rollback();
			}
		} catch (Exception e) {
			LOGGER.severe("Set primary email failed: " + e.getMessage());
			if (con != null) {
				con.rollback();
			}
			throw e;
		} finally {
			if (con != null) {
				con.close();
			}
		}

		return updated;
	}

	public boolean setPrimaryPhone(String phone) throws Exception {
		Connection con = null;
		boolean updated = false;

		try {
			con = DBConnection.getConnection();
			con.setAutoCommit(false);

			Long userId = UserSessionContext.getId();

			if (!userPhoneDao.isPhoneOwnedByUser(con, userId, phone)) {
				throw new Exception("Phone number not found.");
			}

			if (userPhoneDao.isPrimaryExists(con, phone)) {
				throw new Exception("Phone number is already set as primary.");
			}

			boolean reset = userPhoneDao.resetPrimary(con, userId);
			boolean set = userPhoneDao.setPrimary(con, userId, phone);
			boolean statusUpdated = userDao.updateUserStatus(con, userId, UserStatus.VERIFIED);

			if (reset && set && statusUpdated) {
				con.commit();
				updated = true;
				LOGGER.info("Primary phone updated. userId=" + userId);
			} else {
				con.rollback();
			}
		} catch (Exception e) {
			LOGGER.severe("Set primary phone failed: " + e.getMessage());
			if (con != null) {
				con.rollback();
			}
			throw e;
		} finally {
			if (con != null) {
				con.close();
			}
		}

		return updated;
	}

	public boolean deleteSecondaryEmail(String email) throws Exception {
		Connection con = null;
		boolean deleted = false;

		try {
			con = DBConnection.getConnection();
			con.setAutoCommit(false);

			Long userId = UserSessionContext.getId();

			if (!userEmailDao.isEmailOwnedByUser(con, userId, email)) {
				throw new Exception("Email not found.");
			}

			deleted = userEmailDao.delete(con, userId, email);

			if (deleted) {
				con.commit();
				LOGGER.info("Secondary email deleted. userId=" + userId);
			} else {
				con.rollback();
				throw new Exception("Cannot delete Primary Email Address.");
			}
		} catch (Exception e) {
			LOGGER.severe("Delete secondary email failed: " + e.getMessage());
			if (con != null) {
				con.rollback();
			}
			throw e;
		} finally {
			if (con != null) {
				con.close();
			}
		}

		return deleted;
	}

	public boolean deleteSecondaryPhone(String phone) throws Exception {
		Connection con = null;
		boolean deleted = false;

		try {
			con = DBConnection.getConnection();
			con.setAutoCommit(false);

			Long userId = UserSessionContext.getId();

			if (!userPhoneDao.isPhoneOwnedByUser(con, userId, phone)) {
				throw new Exception("Phone number not found.");
			}

			deleted = userPhoneDao.delete(con, userId, phone);

			if (deleted) {
				con.commit();
				LOGGER.info("Secondary phone deleted. userId=" + userId);
			} else {
				con.rollback();
				throw new Exception("Cannot delete Primary Phone number.");
			}
		} catch (Exception e) {
			LOGGER.severe("Delete secondary phone failed: " + e.getMessage());
			if (con != null) {
				con.rollback();
			}
			throw e;
		} finally {
			if (con != null) {
				con.close();
			}
		}

		return deleted;
	}
}