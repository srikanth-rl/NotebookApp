package com.zoho.notebook.dao;

import com.zoho.notebook.model.UserModel;
import com.zoho.notebook.enums.TableName;
import com.zoho.notebook.enums.UserStatus;
import com.zoho.notebook.util.Condition;
import com.zoho.notebook.util.QueryExecutor;
import com.zoho.notebook.util.QueryGenerator;
import com.zoho.notebook.util.FrequentlyUsedMethods;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;

public class UserDao {

    public Long insertUser(Connection con, UserModel user) throws Exception {
        QueryGenerator q = new QueryGenerator();
        long now = System.currentTimeMillis();
        
        q.insertInto(TableName.user.name(), "display_name, status, timezone, created_at, updated_at");
        q.values(5);
        q.addParam(user.getDisplayName());
        q.addParam(UserStatus.PENDING.name());
        q.addParam(user.getTimeZone() != null ? user.getTimeZone() : "UTC");
        q.addParam(now);
        q.addParam(now);
        
        return QueryExecutor.executeInsert(con, q);
    }
    
    public UserModel loginByEmail(Connection con, String email, String pwd) throws Exception {
        QueryGenerator q = new QueryGenerator();
        q.select("u.id, u.display_name, u.timezone");
        q.from("user u");
        q.join("user_emails ue", "u.id = ue.user_id");
        q.join("user_password up", "u.id = up.user_id");
        q.where(List.of(
            new Condition("ue.email", "=", email),
            new Condition("up.password_hash", "=", pwd)
        ), "AND");
        
        try (ResultSet rs = QueryExecutor.executeQuery(con, q)) {
            if (!rs.next()) {
                return null;
            }
            return setAndGetUserBasicDetails(rs);
        }
    }

    public UserModel loginByPhone(Connection con, String phone, String pwd) throws Exception {
        QueryGenerator q = new QueryGenerator();
        q.select("u.id, u.display_name, u.timezone");
        q.from("user u");
        q.join("user_phones uph", "u.id = uph.user_id");
        q.join("user_password up", "u.id = up.user_id");
        q.where(List.of(
            new Condition("uph.phone_number", "=", phone),
            new Condition("up.password_hash", "=", pwd)
        ), "AND");
        
        try (ResultSet rs = QueryExecutor.executeQuery(con, q)) {
            if (!rs.next()) {
                return null;
            }
            return setAndGetUserBasicDetails(rs);
        }
    }

    private UserModel setAndGetUserBasicDetails(ResultSet rs) throws Exception {
        UserModel user = new UserModel();
        user.setId(rs.getLong("id"));
        user.setDisplayName(rs.getString("display_name"));
        user.setTimeZone(rs.getString("timezone"));
        
        return user;
    }

    public UserModel getUserFullDetails(Connection con, Long userId) throws Exception {
        QueryGenerator q = new QueryGenerator();
        q.select("u.id, u.display_name, u.status, u.created_at, u.updated_at, u.timezone");
        q.from("user u");
        q.where(List.of(
            new Condition("u.id", "=", userId)
        ), "AND");

        try (ResultSet rs = QueryExecutor.executeQuery(con, q)) {
            if (!rs.next()) {
                return null;
            }

            UserModel user = setAndGetUserBasicDetails(rs);
            String statusStr = rs.getString("status");
            
            if (statusStr != null) {
                user.setStatus(UserStatus.valueOf(statusStr).name());
            }

            String userTimeZone = user.getTimeZone();
            
            user.setCreatedAt(FrequentlyUsedMethods.formatMsToTimeZone(rs.getLong("created_at"), userTimeZone));
            user.setUpdatedAt(FrequentlyUsedMethods.formatMsToTimeZone(rs.getLong("updated_at"), userTimeZone));
            
            return user;
        }
    }
    
    public boolean updateUserStatus(Connection con, long userId, UserStatus status) throws Exception {
        QueryGenerator q = new QueryGenerator();

        q.update(TableName.user.name());
        q.set(List.of("status", "updated_at"), 
        	  List.of(status.name(), System.currentTimeMillis())
        	);
        q.where(List.of(
            new Condition("id", "=", userId)
        ), "AND");
        
        int rows = QueryExecutor.executeUpdate(con, q);
        return rows > 0;
    }
    
    public boolean updateUserProfile(Connection con, long userId, String displayName, String timeZone) throws Exception {
        QueryGenerator q = new QueryGenerator();

        q.update(TableName.user.name());
        q.set(List.of("display_name", "timezone", "updated_at"), 
              List.of(displayName, timeZone, System.currentTimeMillis())
        	);
        q.where(List.of(
            new Condition("id", "=", userId)
        ), "AND");
        
        int rows = QueryExecutor.executeUpdate(con, q);
        return rows > 0;
    }
}