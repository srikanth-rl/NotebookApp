package com.zoho.notebook.dao;

import com.adventnet.ds.query.Column;
import com.adventnet.ds.query.Criteria;
import com.adventnet.ds.query.QueryConstants;
import com.adventnet.ds.query.SelectQueryImpl;
import com.adventnet.ds.query.Table;
import com.adventnet.ds.query.DataSet;
import com.adventnet.db.api.RelationalAPI;

import com.zoho.notebook.enums.TableName;
import com.zoho.notebook.model.SystemConfigModel;

import java.sql.Connection;

public class SystemConfigDao {

    public SystemConfigModel getConfigByKey(String key) throws Exception {
        SelectQueryImpl q = new SelectQueryImpl(new Table(TableName.SystemConfig.name()));
        q.addSelectColumn(new Column(TableName.SystemConfig.name(), "CONFIG_KEY"));
        q.addSelectColumn(new Column(TableName.SystemConfig.name(), "CONFIG_VALUE"));
        q.addSelectColumn(new Column(TableName.SystemConfig.name(), "CONFIG_TYPE"));
        q.setCriteria(new Criteria(new Column(TableName.SystemConfig.name(), "CONFIG_KEY"), key, QueryConstants.EQUAL));

        RelationalAPI api = RelationalAPI.getInstance();
        Connection conn = api.getConnection();
        try (DataSet ds = api.executeQuery(q, conn)) {
            if (ds.next()) {
                SystemConfigModel config = new SystemConfigModel();
                config.setConfigKey((String) ds.getValue("CONFIG_KEY"));
                config.setConfigValue((String) ds.getValue("CONFIG_VALUE"));
                config.setConfigType((String) ds.getValue("CONFIG_TYPE"));
                return config;
            }
        } finally {
            conn.close();
        }
        return null;
    }
}
