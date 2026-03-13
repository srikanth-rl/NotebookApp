package com.zoho.notebook.dao;

import com.zoho.notebook.model.UserModel;
import com.zoho.notebook.model.UserSessionModel;
import com.zoho.notebook.enums.TableName;
import com.zoho.notebook.util.Condition;
import com.zoho.notebook.util.QueryExecutor;
import com.zoho.notebook.util.QueryGenerator;

import java.util.List;
import java.sql.Connection;
import java.sql.ResultSet;

public class UserSessionDao {

    public boolean insertSessionInDb(Connection con, UserSessionModel sessionData) throws Exception {
	    QueryGenerator q = new QueryGenerator();
	    long newExpiry = System.currentTimeMillis() + (sessionData.getExpiresAt() * 60L * 1000L); // mins to milli seconds
	    
	    q.insertInto(TableName.user_sessions.name(), "session_id, user_id, ip_address, user_agent, expires_at, created_at");
	    q.values(6);
	    q.addParam(sessionData.getSessionId());
	    q.addParam(sessionData.getUserId());
	    q.addParam(sessionData.getIpAddress());
	    q.addParam(sessionData.getUserAgent());
	    q.addParam(newExpiry);
	    q.addParam(sessionData.getCreatedAt()); 
	    
	    return QueryExecutor.executeInsert(con, q) > 0;
	}
    
    public boolean updateExpiryTimeInDb(Connection con, String sessionId, int timeoutMins) throws Exception {
        QueryGenerator q = new QueryGenerator();
        long newExpiry = System.currentTimeMillis() + (timeoutMins * 60L * 1000L);

        q.update(TableName.user_sessions.name());
        q.set(
        		List.of("expires_at"), 
        		List.of(newExpiry)
        	);
        q.where(List.of(
        		new Condition("session_id", "=", sessionId)
        	), "AND");

        return QueryExecutor.executeUpdate(con, q) > 0;
    }
    
    public UserModel validateSessionAndGetUserFromDb(Connection con, String sessionId) throws Exception {
        QueryGenerator q = new QueryGenerator();
        long now = System.currentTimeMillis();
        
        q.select("u.id, u.display_name, u.timezone");
        q.from(TableName.user.name() +" u");
        q.join(TableName.user_sessions.name() + " s", "u.id = s.user_id");
        q.where(List.of(
            new Condition("s.session_id", "=", sessionId),
            new Condition("s.expires_at", ">", now)
        ), "AND");

        try (ResultSet rs = QueryExecutor.executeQuery(con, q)) {
            if (rs.next()) {
                UserModel user = new UserModel();
                user.setId(rs.getLong("id"));
                user.setDisplayName(rs.getString("display_name"));
                user.setTimeZone(rs.getString("timezone"));
                return user;
            }
        }
        return null;
    }

    public boolean deleteSessionFromDb(Connection con, String sessionId) throws Exception {
    	QueryGenerator q = new QueryGenerator();

    	q.delete(TableName.user_sessions.name());
    	q.where(List.of(new Condition("session_id", "=", sessionId)), "AND");

    	int rows = QueryExecutor.executeUpdate(con, q);
    	return rows > 0;
    }
}