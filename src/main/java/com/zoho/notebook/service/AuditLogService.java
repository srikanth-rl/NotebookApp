package com.zoho.notebook.service;

import com.zoho.notebook.dao.AuditLogDao;
import java.sql.Connection;
import java.util.logging.Logger;

public class AuditLogService {
    private static final AuditLogDao auditLogDao = new AuditLogDao();
    private static final Logger LOGGER = Logger.getLogger(AuditLogService.class.getName());

    public static void log(Connection con, Long userId, String table, Long targetId, 
                           String action, String oldData, String newData, String ip) {
        try {
            // DAO call must handle these 8 arguments
            auditLogDao.insertAudit(con, userId, table, targetId, action, oldData, newData, ip);
        } catch (Exception e) {
            LOGGER.severe("Audit log insert failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}