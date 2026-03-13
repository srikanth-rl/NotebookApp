package com.zoho.notebook.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class HikariCPManager {

	private static final Map<String, HikariDataSource> dataSourceMap = new ConcurrentHashMap<>();
	private static Properties dbProps = new Properties();

	static {
		try {
			dbProps.load(HikariCPManager.class.getClassLoader().getResourceAsStream("db.properties"));
		} catch (Exception e) {
			throw new RuntimeException("Error loading db.properties", e);
		}
	}

	public static Connection getConnection(String ipAddress) throws SQLException {
		if (ipAddress == null || ipAddress.isBlank()) {
			throw new SQLException("Database IP Address is required!");
		}
		return dataSourceMap.computeIfAbsent(ipAddress, HikariCPManager::createDataSource).getConnection(); // One pool per IP
	}

	private static HikariDataSource createDataSource(String ip) {
		HikariConfig config = new HikariConfig();

		String urlTemplate = dbProps.getProperty("db.url.template");
		String finalUrl = String.format(urlTemplate, ip);

		config.setJdbcUrl(finalUrl);
		config.setUsername(dbProps.getProperty("db.username"));
		config.setPassword(dbProps.getProperty("db.password"));
		config.setDriverClassName(dbProps.getProperty("db.driver"));

		// --- Pool Settings ---
		config.setPoolName("HikariPool-" + ip);

		config.setMaximumPoolSize(10); // 10 connections
		config.setConnectionTimeout(30000); // 30 seconds
		config.setIdleTimeout(600000); // 10 minutes
		config.setMaxLifetime(1800000); // 30 minutes

		return new HikariDataSource(config);
	}
}