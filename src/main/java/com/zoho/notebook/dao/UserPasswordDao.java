package com.zoho.notebook.dao;

import com.zoho.notebook.enums.TableName;
import com.zoho.notebook.util.QueryExecutor;
import com.zoho.notebook.util.QueryGenerator;

import java.sql.Connection;

public class UserPasswordDao {

    public boolean insert(Connection con, long userId, String pwd) throws Exception {
    	QueryGenerator q = new QueryGenerator();
    	long now = System.currentTimeMillis();
    	
        q.insertInto(TableName.user_password.name(), "user_id, password_hash, created_at, updated_at");
        q.values(4);
        q.addParam(userId);
        q.addParam(pwd);
        q.addParam(now);
        q.addParam(now);
        
        int rows = QueryExecutor.executeUpdate(con, q);        
        return rows > 0;
    }
}