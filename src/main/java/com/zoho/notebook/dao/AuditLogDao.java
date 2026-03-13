package com.zoho.notebook.dao;

import com.zoho.notebook.enums.TableName;
import com.zoho.notebook.util.Condition;
import com.zoho.notebook.util.QueryExecutor;
import com.zoho.notebook.util.QueryGenerator;
import com.zoho.notebook.model.AuditLogModel;

import java.util.List;
import java.sql.ResultSet;
import java.sql.Connection;
import java.util.ArrayList;

@SuppressWarnings("unused")
public class AuditLogDao {

    public boolean insertAudit(Connection con, long userId, String targetTable, long targetId,
                            String action, String oldData, String newData, String ip) throws Exception {

        QueryGenerator q = new QueryGenerator();
        long now = System.currentTimeMillis();

        q.insertInto(TableName.audit_logs.name(),
                "user_id, target_table, target_id, action, old_data, new_data, ip_address, created_at");
        q.values(8);

        q.addParam(userId);
        q.addParam(targetTable);
        q.addParam(targetId);
        q.addParam(action);
        q.addParam(oldData);
        q.addParam(newData);
        q.addParam(ip);
        q.addParam(now);

        Long generatedId = QueryExecutor.executeInsert(con, q);
        return generatedId != null && generatedId > 0;
    }

/*
    public List<AuditLogModel> getAuditHistory(Connection con, String targetTable, long targetId) throws Exception {

        List<AuditLogModel> history = new ArrayList<>();
        QueryGenerator q = new QueryGenerator();

        q.select("id, user_id, target_table, target_id, action, old_data, new_data, ip_address, created_at");
        q.from(TableName.audit_logs.name());
        q.where(List.of(
                new Condition("target_table", "=", targetTable),
                new Condition("target_id", "=", targetId)
        ), "AND");
        q.orderBy(List.of("created_at"));

        try (ResultSet rs = QueryExecutor.executeQuery(con, q)) {

            while (rs.next()) {

                AuditLogModel log = new AuditLogModel();

                log.setId(rs.getLong("id"));
                log.setUserId(rs.getLong("user_id"));
                log.setTargetTable(rs.getString("target_table"));
                log.setTargetId(rs.getLong("target_id"));
                log.setAction(rs.getString("action"));
                log.setOldData(rs.getString("old_data"));
                log.setNewData(rs.getString("new_data"));
                log.setIpAddress(rs.getString("ip_address"));
                log.setCreatedAt(rs.getLong("created_at"));

                history.add(log);
            }
        }

        return history;
    }
*/
}
