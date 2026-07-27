package com.vyay.core.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

@Component
public class DataSourceLogger {

    private final DataSource dataSource;

    public DataSourceLogger(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void logDatabaseInfo() {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            System.out.println("✅ Driver Name: " + metaData.getDriverName());
            System.out.println("✅ Driver Version: " + metaData.getDriverVersion());
            System.out.println("✅ DB Product: " + metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion());
            System.out.println("✅ AutoCommit: " + conn.getAutoCommit());
            System.out.println("✅ Isolation: " + conn.getTransactionIsolation());
        } catch (Exception e) {
            System.err.println("⚠️ Failed to log DB info: " + e.getMessage());
        }
    }
}
