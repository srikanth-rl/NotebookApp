package com.zoho.notebook.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.zoho.notebook.enums.TableName;
import com.zoho.notebook.model.UserPhoneModel;
import com.zoho.notebook.util.Condition;
import com.zoho.notebook.util.QueryExecutor;
import com.zoho.notebook.util.QueryGenerator;
import com.zoho.notebook.util.FrequentlyUsedMethods;

public class UserPhoneDao {

    public boolean insert(Connection con, long userId, String phone, int isPrimary) throws Exception {
        QueryGenerator q = new QueryGenerator();
        long now = System.currentTimeMillis();
        
        q.insertInto(TableName.user_phones.name(), "user_id, phone_number, is_primary, created_at, updated_at");
        q.values(5);
        q.addParam(userId);
        q.addParam(phone);
        q.addParam(isPrimary);
        q.addParam(now);
        q.addParam(now);

        Long id = QueryExecutor.executeInsert(con, q);
        
        return id != null;
    }

    public List<UserPhoneModel> getPhoneDetailsByUserId(Connection con, Long userId, String userTimeZone) throws Exception {
        List<UserPhoneModel> phones = new ArrayList<>();
        QueryGenerator q = new QueryGenerator();
        
        q.select("id, phone_number, is_primary, created_at, updated_at");
        q.from(TableName.user_phones.name());
        q.where(List.of(
            new Condition("user_id", "=", userId)
        ), "AND");
        q.orderBy(List.of("is_primary DESC", "updated_at DESC"));

        try (ResultSet rs = QueryExecutor.executeQuery(con, q)) {
            while (rs.next()) {
                UserPhoneModel phone = new UserPhoneModel();
                phone.setId(rs.getLong("id"));
                String dbPhone = rs.getString("phone_number"); // Format: "91|9876543210"
                
                if (dbPhone != null && dbPhone.contains("|")) {
                    String[] parts = dbPhone.split("\\|");
                    phone.setCountryCode(parts[0]); // "91"
                    phone.setPhoneNumber(parts[1]);  // "9876543210"
                } else {
                    // Fallback for actual data or single values
                    phone.setPhoneNumber(dbPhone);
                    phone.setCountryCode(null); 
                }
                phone.setPrimary(rs.getInt("is_primary") == 1);
                phone.setCreatedAt(FrequentlyUsedMethods.formatMsToTimeZone(rs.getLong("created_at"), userTimeZone));
                phone.setUpdatedAt(FrequentlyUsedMethods.formatMsToTimeZone(rs.getLong("updated_at"), userTimeZone));
                
                phones.add(phone);
            }
        }
        
        return phones;
    }
    
    public boolean isPhoneOwnedByUser(Connection con, long userId, String phone) throws Exception {
        QueryGenerator q = new QueryGenerator();
        
        q.select("1");
        q.from(TableName.user_phones.name());
        q.where(List.of(
            new Condition("user_id", "=", userId),
            new Condition("phone_number", "=", phone)
        ), "AND");
        
        try (ResultSet rs = QueryExecutor.executeQuery(con, q)) {
            return rs.next();
        }
    }

    public boolean isPrimaryExists(Connection con, String phone) throws Exception {
        QueryGenerator q = new QueryGenerator();
        
        q.select("1");
        q.from(TableName.user_phones.name());
        q.where(List.of(
            new Condition("phone_number", "=", phone),
            new Condition("is_primary", "=", 1)
        ), "AND");
        
        try (ResultSet rs = QueryExecutor.executeQuery(con, q)) {
            return rs.next();
        }
    }

    public boolean resetPrimary(Connection con, long userId) throws Exception {
        QueryGenerator q = new QueryGenerator();
        
        q.update(TableName.user_phones.name());
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

    public boolean setPrimary(Connection con, long userId, String phone) throws Exception {
        QueryGenerator q = new QueryGenerator();
        
        q.update(TableName.user_phones.name());
        q.set(
            List.of("is_primary", "updated_at"), 
            List.of(1, System.currentTimeMillis())
        );
        q.where(List.of(
            new Condition("user_id", "=", userId),
            new Condition("phone_number", "=", phone)
        ), "AND");
        
        int rows = QueryExecutor.executeUpdate(con, q);
        return rows > 0;
    }
    
    public boolean delete(Connection con, long userId, String phone) throws Exception {
        QueryGenerator q = new QueryGenerator();
        
        q.delete(TableName.user_phones.name());
        q.where(List.of(
            new Condition("user_id", "=", userId),
            new Condition("phone_number", "=", phone),
            new Condition("is_primary", "=", 0)
        ), "AND");

        int rowsAffected = QueryExecutor.executeUpdate(con, q);
        return rowsAffected > 0;
    }
}