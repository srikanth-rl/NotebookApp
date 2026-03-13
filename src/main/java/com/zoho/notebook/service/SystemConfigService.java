package com.zoho.notebook.service;

import com.zoho.notebook.dao.SystemConfigDao;
import com.zoho.notebook.model.SystemConfigModel;
import com.zoho.notebook.util.DBConnection;
import java.sql.Connection;
import java.util.logging.Logger;

public class SystemConfigService {

    private final SystemConfigDao configDao = new SystemConfigDao();
    private static final Logger LOGGER = Logger.getLogger(SystemConfigService.class.getName());
    
    private Object getParsedValue(String key) {
        try (Connection con = DBConnection.getConnection()) {
            SystemConfigModel config = configDao.getConfigByKey(con, key);
            
            if (config == null || config.getConfigValue() == null) {
                return null;
            }

            String value = config.getConfigValue().trim();
            String type = config.getConfigType(); // INT, BOOLEAN, STRING

            switch (type.toUpperCase()) {
                case "INT":
                    return Integer.parseInt(value);
                case "BOOLEAN":
                    return Boolean.parseBoolean(value);
                case "STRING":
                default:
                    return value;
            }
        } catch (Exception e) {
            LOGGER.severe("Failed to fetch system config for key: " + key + " | Error: " + e.getMessage());
            return null;
        }
    }

    public int getInt(String key, int defaultValue) {
        Object val = getParsedValue(key);
        return (val instanceof Integer) ? (Integer) val : defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        Object val = getParsedValue(key);
        return (val instanceof Boolean) ? (Boolean) val : defaultValue;
    }

    public String getString(String key, String defaultValue) {
        Object val = getParsedValue(key);
        return (val instanceof String) ? (String) val : defaultValue;
    }
}