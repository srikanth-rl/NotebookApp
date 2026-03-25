package com.zoho.notebook.util;

import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.zoho.notebook.enums.ActionType;
import com.zoho.notebook.service.AuditLogService;
import com.zoho.notebook.context.UserSessionContext;

public class QueryExecutor {

    public static ResultSet executeQuery(Connection con, QueryGenerator qg) throws Exception {
        PreparedStatement ps = con.prepareStatement(qg.build());
        bindParams(ps, qg);
        return ps.executeQuery();
    }

    public static Long executeInsert(Connection con, QueryGenerator qg) throws Exception {
        String sql = qg.build();
        Long id = null;
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindParams(ps, qg);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) 
                	id = rs.getLong(1);
            }
        }
        
        String table = extractTableName(sql, ActionType.INSERT.name());
        // Only audit if it's a loggable table and we have a target ID
        if (id != null && isLoggableTable(table)) {
            performAudit(con, table, id, ActionType.INSERT.name(), null);
        }
        return id;
    }

    public static int executeUpdate(Connection con, QueryGenerator qg) throws Exception {
        String sql = qg.build();
        String action = sql.trim().toUpperCase().startsWith(ActionType.DELETE.name()) ? ActionType.DELETE.name() : ActionType.UPDATE.name();
        String table = extractTableName(sql, action);
        
        // If it's a session or audit table, just execute update and skip logging
        if (!isLoggableTable(table)) {
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                bindParams(ps, qg);
                return ps.executeUpdate();
            }
        }

        Long targetId = qg.getId();
        String oldData = fetchRowAsJson(con, table, targetId);

        int rows;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            bindParams(ps, qg);
            rows = ps.executeUpdate();
        }

        if (rows > 0) {
            performAudit(con, table, targetId, action, oldData);
        }
        return rows;
    }

    private static void performAudit(Connection con, String table, Long id, String action, String oldData) {
        try {
            // If the user isn't logged in yet (userId is null)
            Long userId = UserSessionContext.getId();
            if (userId == null) {
                return; 
            }

            String newData = ActionType.DELETE.name().equals(action) ? null : 
            					fetchRowAsJson(con, table, id);
            
            // Don't log if the data is exactly the same after the update
            if (ActionType.UPDATE.name().equals(action) && newData != null && newData.equals(oldData)) {
                return;
            }

            AuditLogService.log(
//                con, 
                userId,
                table,
                id,
                action, 
                oldData, 
                newData, 
                UserSessionContext.getClientIp()
            );
        } catch (Exception e) {
            System.out.println("Audit Log Transaction failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean isLoggableTable(String table) {
        if (table == null) return false;
        // Explicitly ignore tables
        String t = table.toLowerCase();
        return !t.equals("audit_logs") && !t.equals("user_sessions");
    }

    private static String fetchRowAsJson(Connection con, String table, Long id) {
        if (id == null || table == null) return null;
        String sql = "SELECT * FROM " + table + " WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) 
                	return ResponseMapper.resultSetToJson(rs);
            }
        } catch (Exception e) { 
        	return null; 
        }
        return null;
    }

    private static String extractTableName(String sql, String action) {
        if (sql == null) 
        	return null;
        String tableNamePattern = "\\s+([\\w\\.]+)";
        String regex = action.contains(ActionType.INSERT.name()) ? "INSERT INTO" + tableNamePattern : 
                       action.contains(ActionType.UPDATE.name()) ? ActionType.UPDATE.name() + tableNamePattern : 
                       "DELETE FROM" + tableNamePattern;
        
        Matcher m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(sql);
        return m.find() ? m.group(1).toLowerCase() : null;
    }
    
    private static void bindParams(PreparedStatement ps, QueryGenerator qg) throws Exception {
        int idx = 1;
        for (Object param : qg.getParams()) ps.setObject(idx++, param);
    }
}