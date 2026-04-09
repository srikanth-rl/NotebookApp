package com.zoho.notebook.dao;

import com.adventnet.ds.query.Column;
import com.adventnet.ds.query.Criteria;
import com.adventnet.ds.query.Join;
import com.adventnet.ds.query.QueryConstants;
import com.adventnet.ds.query.SelectQueryImpl;
import com.adventnet.ds.query.Table;
import com.adventnet.ds.query.UpdateQueryImpl;
import com.adventnet.ds.query.DataSet;
import com.adventnet.db.api.RelationalAPI;
import com.adventnet.persistence.DataAccess;
import com.adventnet.persistence.DataObject;
import com.adventnet.persistence.Row;
import com.adventnet.persistence.WritableDataObject;

import com.zoho.notebook.enums.TableName;
import com.zoho.notebook.model.UserModel;
import com.zoho.notebook.model.UserSessionModel;

import java.sql.Connection;

public class UserSessionDao {

    public boolean insertSessionInDb(UserSessionModel sessionData) throws Exception {
	    long newExpiry = System.currentTimeMillis() + (sessionData.getExpiresAt() * 60L * 1000L);
	
	    String userAgent = sessionData.getUserAgent();
	    if (userAgent != null && userAgent.length() > 50)
	        userAgent = userAgent.substring(0, 50);
	
	    Row r = new Row(TableName.UserSession.name());
	    r.set("SESSION_ID", sessionData.getSessionId());
	    r.set("USER_ID",    sessionData.getUserId());
	    r.set("IP_ADDRESS", sessionData.getIpAddress());
	    r.set("USER_AGENT", userAgent);
	    r.set("EXPIRES_AT", newExpiry);
	    r.set("CREATED_AT", sessionData.getCreatedAt());
	
	    DataObject d = new WritableDataObject();
	    d.addRow(r);
	    DataAccess.add(d);
	    return d.getRow(TableName.UserSession.name()) != null;
	}

    public boolean updateExpiryTimeInDb(String sessionId, int timeoutMins) throws Exception {
        long newExpiry = System.currentTimeMillis() + (timeoutMins * 60L * 1000L);

        UpdateQueryImpl q = new UpdateQueryImpl(TableName.UserSession.name());
        q.setUpdateColumn("EXPIRES_AT", newExpiry);
        q.setCriteria(new Criteria(new Column(TableName.UserSession.name(), "SESSION_ID"), sessionId, QueryConstants.EQUAL));
        DataAccess.update(q);
        return true;
    }

    public UserModel validateSessionAndGetUserFromDb(String sessionId) throws Exception {
        long now = System.currentTimeMillis();

        SelectQueryImpl q = new SelectQueryImpl(new Table(TableName.User.name(), "u"));
        q.addSelectColumn(new Column("u", "ID"));
        q.addSelectColumn(new Column("u", "DISPLAY_NAME"));
        q.addSelectColumn(new Column("u", "TIMEZONE"));
        q.addJoin(new Join(TableName.User.name(), TableName.UserSession.name(), new String[]{"ID"}, new String[]{"USER_ID"}, "u", "s", Join.INNER_JOIN));
        Criteria c = new Criteria(new Column("s", "SESSION_ID"), sessionId, QueryConstants.EQUAL);
        c = c.and(new Criteria(new Column("s", "EXPIRES_AT"),    now,       QueryConstants.GREATER_THAN));
        q.setCriteria(c);

        RelationalAPI api = RelationalAPI.getInstance();
        Connection conn = api.getConnection();
        try (DataSet ds = api.executeQuery(q, conn)) {
            if (ds.next()) {
                UserModel user = new UserModel();
                user.setId(((Number) ds.getValue("ID")).longValue());
                user.setDisplayName((String) ds.getValue("DISPLAY_NAME"));
                user.setTimezone((String) ds.getValue("TIMEZONE"));
                return user;
            }
        } finally {
            conn.close();
        }
        return null;
    }

    public boolean deleteSessionFromDb(String sessionId) throws Exception {
        Criteria c = new Criteria(new Column(TableName.UserSession.name(), "SESSION_ID"), sessionId, QueryConstants.EQUAL);
        DataAccess.delete(c);
        return true;
    }
}
