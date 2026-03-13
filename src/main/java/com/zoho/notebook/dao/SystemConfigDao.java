package com.zoho.notebook.dao;

import com.zoho.notebook.model.SystemConfigModel;
import com.zoho.notebook.enums.TableName;
import com.zoho.notebook.util.Condition;
import com.zoho.notebook.util.QueryExecutor;
import com.zoho.notebook.util.QueryGenerator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;

public class SystemConfigDao {

    public SystemConfigModel getConfigByKey(Connection con, String key) throws Exception {
        QueryGenerator q = new QueryGenerator();
        
        q.select("config_key, config_value, config_type");
        q.from(TableName.system_configs.name());
        q.where(List.of(new Condition("config_key", "=", key)), "AND");

        try (ResultSet rs = QueryExecutor.executeQuery(con, q)) {
            if (rs.next()) {
                SystemConfigModel config = new SystemConfigModel();
                config.setConfigKey(rs.getString("config_key"));
                config.setConfigValue(rs.getString("config_value"));
                config.setConfigType(rs.getString("config_type"));
                return config;
            }
        }

        return null;
    }
}
