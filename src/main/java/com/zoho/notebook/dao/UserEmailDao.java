package com.zoho.notebook.dao;

import com.zoho.notebook.enums.TableName;
import com.zoho.notebook.model.UserEmailModel;
import com.zoho.notebook.util.Condition;
import com.zoho.notebook.util.FrequentlyUsedMethods;
import com.zoho.notebook.util.QueryExecutor;
import com.zoho.notebook.util.QueryGenerator;

import java.sql.ResultSet;
import java.sql.Connection;
import java.util.List;
import java.util.ArrayList;

public class UserEmailDao {

    public boolean insert(Connection con, long userId, String email, int isPrimary) throws Exception {
        QueryGenerator q = new QueryGenerator();
        long now = System.currentTimeMillis();
        
        q.insertInto(TableName.user_emails.name(), "user_id, email, is_primary, created_at, updated_at");
        q.values(5);
        q.addParam(userId);
        q.addParam(email);
        q.addParam(isPrimary);
        q.addParam(now);
        q.addParam(now);

        Long id = QueryExecutor.executeInsert(con, q);
        
        return id != null;
    }

    public List<UserEmailModel> getEmailDetailsByUserId(Connection con, Long userId, String userTimeZone) throws Exception {
        List<UserEmailModel> emails = new ArrayList<>();
        QueryGenerator q = new QueryGenerator();
        
        q.select("id, email, is_primary, created_at, updated_at");
        q.from(TableName.user_emails.name());
        q.where(List.of(
            new Condition("user_id", "=", userId)
        ), "AND");
        q.orderBy(List.of("is_primary DESC", "updated_at DESC"));
        
        try (ResultSet rs = QueryExecutor.executeQuery(con, q)) {
            while (rs.next()) {
                UserEmailModel email = new UserEmailModel();
                email.setId(rs.getLong("id"));
                email.setEmail(rs.getString("email"));
                email.setPrimary(rs.getInt("is_primary") == 1);
                email.setCreatedAt(FrequentlyUsedMethods.formatMsToTimeZone(rs.getLong("created_at"), userTimeZone));
                email.setUpdatedAt(FrequentlyUsedMethods.formatMsToTimeZone(rs.getLong("updated_at"), userTimeZone));      
                
                emails.add(email);
            }
        }
        
        return emails;
    }
    
    public boolean isEmailTaken(Connection con, String email) throws Exception {
        QueryGenerator q = new QueryGenerator();
       
        q.select("1");
        q.from(TableName.user_emails.name());
        q.where(List.of(
            new Condition("email", "=", email)
        ), "AND");
        
        try (ResultSet rs = QueryExecutor.executeQuery(con, q)) {
            return rs.next();
        }
    }
    
    public boolean isEmailOwnedByUser(Connection con, long userId, String email) throws Exception {
        QueryGenerator q = new QueryGenerator();
       
        q.select("1");
        q.from(TableName.user_emails.name());
        q.where(List.of(
        	new Condition("user_id", "=", userId),
            new Condition("email", "=", email)
        ), "AND");
        
        try (ResultSet rs = QueryExecutor.executeQuery(con, q)) {
            return rs.next();
        }
    }
    
    public boolean isPrimaryExists(Connection con, String email) throws Exception {
        QueryGenerator q = new QueryGenerator();
        
        q.select("1");
        q.from(TableName.user_emails.name());
        q.where(List.of(
            new Condition("email", "=", email),
            new Condition("is_primary", "=", 1)
        ), "AND");
        
        try (ResultSet rs = QueryExecutor.executeQuery(con, q)) {
            return rs.next();
        }
    }

    public boolean resetPrimary(Connection con, long userId) throws Exception {
        QueryGenerator q = new QueryGenerator();
        
        q.update(TableName.user_emails.name());
        q.set(
            List.of("is_primary", "updated_at"), 
            List.of(0, System.currentTimeMillis())
        );
        
        q.where(List.of(
            new Condition("user_id", "=", userId)
        ), "AND");
        
        int rows = QueryExecutor.executeUpdate(con, q);
        return rows > 0;
    }

    public boolean setPrimary(Connection con, long userId, String email) throws Exception {
        QueryGenerator q = new QueryGenerator();
        q.update(TableName.user_emails.name());
        q.set(
            List.of("is_primary", "updated_at"), 
            List.of(1, System.currentTimeMillis())
        );
        
        q.where(List.of(
            new Condition("user_id", "=", userId),
            new Condition("email", "=", email)
        ), "AND");
        
        int rows = QueryExecutor.executeUpdate(con, q);
        return rows > 0;
    }
    
    public boolean delete(Connection con, long userId, String email) throws Exception {
        QueryGenerator q = new QueryGenerator();
        
        q.delete(TableName.user_emails.name());
        q.where(List.of(
            new Condition("user_id", "=", userId),
            new Condition("email", "=", email),
            new Condition("is_primary", "=", 0)
        ), "AND");

        int rows = QueryExecutor.executeUpdate(con, q);
        return rows > 0;
    }
}