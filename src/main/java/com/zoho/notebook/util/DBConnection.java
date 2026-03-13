package com.zoho.notebook.util;

import java.sql.Connection;
// import java.sql.DriverManager;
// import java.util.Properties;

public class DBConnection {

//    private static final Properties props = new Properties();
//
//    static {
//        try {
//            props.load(DBConnection.class
//                    .getClassLoader()
//                    .getResourceAsStream("db.properties"));
//
//            Class.forName(props.getProperty("db.driver"));
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to initialize DB", e);
//        }
//    }

	public static Connection getConnection() throws Exception {
//        return DriverManager.getConnection(
//                props.getProperty("db.url"),
//                props.getProperty("db.username"),
//                props.getProperty("db.password")
//        );
		String ipAddress = "localhost";
		return HikariCPManager.getConnection(ipAddress);
	}
}