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
import com.zoho.notebook.enums.UserStatus;
import com.zoho.notebook.model.UserModel;
import com.zoho.notebook.util.FrequentlyUsedMethods;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class UserDao {

    public Long insertUser(UserModel user) throws Exception {
        long now = System.currentTimeMillis();

        Row r = new Row(TableName.User.name());
        r.set("DISPLAY_NAME", user.getDisplayName());
        r.set("STATUS",       UserStatus.PENDING.name());
        r.set("TIMEZONE",     user.getTimezone() != null ? user.getTimezone() : "UTC");
        r.set("CREATED_AT",   now);
        r.set("UPDATED_AT",   now);

        DataObject d = new WritableDataObject();
        d.addRow(r);
        DataAccess.add(d);

        Row inserted = d.getRow(TableName.User.name());
        if (inserted == null) {
            return null;
        }
        Object id = inserted.get("ID");
        return id != null ? ((Number) id).longValue() : null;
    }

    public UserModel loginByEmail(String email, String pwd) throws Exception {
        SelectQueryImpl q = new SelectQueryImpl(new Table(TableName.User.name(), "u"));
        q.addSelectColumn(new Column("u", "ID"));
        q.addSelectColumn(new Column("u", "DISPLAY_NAME"));
        q.addSelectColumn(new Column("u", "TIMEZONE"));
        q.addJoin(new Join(TableName.User.name(), TableName.UserEmail.name(),    new String[]{"ID"}, new String[]{"USER_ID"}, "u", "ue", Join.INNER_JOIN));
        q.addJoin(new Join(TableName.User.name(), TableName.UserPassword.name(), new String[]{"ID"}, new String[]{"USER_ID"}, "u", "up", Join.INNER_JOIN));
        Criteria c = new Criteria(new Column("ue", "EMAIL"),       email, QueryConstants.EQUAL);
        c = c.and(new Criteria(new Column("up", "PASSWORD_HASH"),  pwd,   QueryConstants.EQUAL));
        q.setCriteria(c);

        RelationalAPI api = RelationalAPI.getInstance();
        Connection conn = api.getConnection();
        try (DataSet ds = api.executeQuery(q, conn)) {
            if (ds.next()) {
                return setAndGetUserBasicDetails(ds);
            }
        } finally {
            conn.close();
        }
        return null;
    }

    public UserModel loginByPhone(String phone, String pwd) throws Exception {
        SelectQueryImpl q = new SelectQueryImpl(new Table(TableName.User.name(), "u"));
        q.addSelectColumn(new Column("u", "ID"));
        q.addSelectColumn(new Column("u", "DISPLAY_NAME"));
        q.addSelectColumn(new Column("u", "TIMEZONE"));
        q.addJoin(new Join(TableName.User.name(), TableName.UserPhone.name(),    new String[]{"ID"}, new String[]{"USER_ID"}, "u", "uph", Join.INNER_JOIN));
        q.addJoin(new Join(TableName.User.name(), TableName.UserPassword.name(), new String[]{"ID"}, new String[]{"USER_ID"}, "u", "up",  Join.INNER_JOIN));
        Criteria c = new Criteria(new Column("uph", "PHONE_NUMBER"), phone, QueryConstants.EQUAL);
        c = c.and(new Criteria(new Column("up",  "PASSWORD_HASH"),   pwd,   QueryConstants.EQUAL));
        q.setCriteria(c);

        RelationalAPI api = RelationalAPI.getInstance();
        Connection conn = api.getConnection();
        try (DataSet ds = api.executeQuery(q, conn)) {
            if (ds.next()) {
                return setAndGetUserBasicDetails(ds);
            }
        } finally {
            conn.close();
        }
        return null;
    }

    private UserModel setAndGetUserBasicDetails(DataSet ds) throws Exception {
        UserModel user = new UserModel();
        user.setId(((Number) ds.getValue("ID")).longValue());
        user.setDisplayName((String) ds.getValue("DISPLAY_NAME"));
        user.setTimezone((String) ds.getValue("TIMEZONE"));
        return user;
    }

    public UserModel getUserFullDetails(Long userId) throws Exception {
        SelectQueryImpl q = new SelectQueryImpl(new Table(TableName.User.name(), "u"));
        q.addSelectColumn(new Column("u", "ID"));
        q.addSelectColumn(new Column("u", "DISPLAY_NAME"));
        q.addSelectColumn(new Column("u", "STATUS"));
        q.addSelectColumn(new Column("u", "TIMEZONE"));
        q.addSelectColumn(new Column("u", "CREATED_AT"));
        q.addSelectColumn(new Column("u", "UPDATED_AT"));
        q.setCriteria(new Criteria(new Column("u", "ID"), userId, QueryConstants.EQUAL));

        RelationalAPI api = RelationalAPI.getInstance();
        Connection conn = api.getConnection();
        try (DataSet ds = api.executeQuery(q, conn)) {
            if (!ds.next()) {
                return null;
            }
            UserModel user = setAndGetUserBasicDetails(ds);
            String statusStr = (String) ds.getValue("STATUS");
            if (statusStr != null) {
                user.setStatus(UserStatus.valueOf(statusStr).name());
            }
            String userTimezone = user.getTimezone();
            user.setCreatedAt(FrequentlyUsedMethods.formatMsToTimezone(((Number) ds.getValue("CREATED_AT")).longValue(), userTimezone));
            user.setUpdatedAt(FrequentlyUsedMethods.formatMsToTimezone(((Number) ds.getValue("UPDATED_AT")).longValue(), userTimezone));
            return user;
        } finally {
            conn.close();
        }
    }

    public boolean updateUserStatus(long userId, UserStatus status) throws Exception {
        UpdateQueryImpl q = new UpdateQueryImpl(TableName.User.name());
        q.setUpdateColumn("STATUS",     status.name());
        q.setUpdateColumn("UPDATED_AT", System.currentTimeMillis());
        q.setCriteria(new Criteria(new Column(TableName.User.name(), "ID"), userId, QueryConstants.EQUAL));
        DataAccess.update(q);
        return true;
    }

    public boolean updateUserProfile(long userId, String displayName, String timeZone) throws Exception {
        UpdateQueryImpl q = new UpdateQueryImpl(TableName.User.name());
        q.setUpdateColumn("DISPLAY_NAME", displayName);
        q.setUpdateColumn("TIMEZONE",     timeZone);
        q.setUpdateColumn("UPDATED_AT",   System.currentTimeMillis());
        q.setCriteria(new Criteria(new Column(TableName.User.name(), "ID"), userId, QueryConstants.EQUAL));
        DataAccess.update(q);
        return true;
    }

    public boolean userExistsById(long userId) throws Exception {
        SelectQueryImpl q = new SelectQueryImpl(new Table(TableName.User.name(), "u"));
        q.addSelectColumn(new Column("u", "ID"));
        q.setCriteria(new Criteria(new Column("u", "ID"), userId, QueryConstants.EQUAL));

        RelationalAPI api = RelationalAPI.getInstance();
        Connection conn = api.getConnection();
        try (DataSet ds = api.executeQuery(q, conn)) {
            return ds.next();
        } finally {
            conn.close();
        }
    }

    public boolean insertUserWithZuid(long zuid, String displayName, String timeZone) throws Exception {
        long now = System.currentTimeMillis();

        RelationalAPI api = RelationalAPI.getInstance();
        Connection conn = api.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO User (ID, DISPLAY_NAME, STATUS, TIMEZONE, CREATED_AT, UPDATED_AT) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setLong(1, zuid);
            ps.setString(2, displayName != null ? displayName : "User");
            ps.setString(3, UserStatus.VERIFIED.name());
            ps.setString(4, timeZone != null ? timeZone : "UTC");
            ps.setLong(5, now);
            ps.setLong(6, now);
            int rows = ps.executeUpdate();
            return rows > 0;
        } finally {
            conn.close();
        }
    }
}
