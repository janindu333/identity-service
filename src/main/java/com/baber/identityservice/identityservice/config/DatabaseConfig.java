package com.baber.identityservice.identityservice.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

@Configuration
@EnableConfigurationProperties({R2dbcProperties.class, FlywayProperties.class})
class DatabaseConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(FlywayProperties flywayProperties, R2dbcProperties r2dbcProperties) {
        String databaseUrl = flywayProperties.getUrl();
        String username = r2dbcProperties.getUsername();
        String password = r2dbcProperties.getPassword();

        System.out.println("Flyway URL: " + databaseUrl);
        System.out.println("Flyway Locations: " + Arrays.toString(flywayProperties.getLocations().toArray(new String[0])));
        System.out.println("R2dbc Username: " + username);
        System.out.println("R2dbc Password: " + password);

        String databaseName = extractDatabaseNameFromUrl(databaseUrl);
        String jdbcUrlWithoutDbName = extractJdbcUrlWithoutDatabaseName(databaseUrl);

        System.out.println("JDBC URL: " + databaseUrl);
        System.out.println("JDBC URL without DB Name: " + jdbcUrlWithoutDbName);
        System.out.println("Database Name: " + databaseName);

        try {
            // Ensure the database exists
            ensureDatabaseExists(jdbcUrlWithoutDbName, databaseName, username, password);

            // Check the database connection before configuring Flyway
            checkDatabaseConnection(databaseUrl, username, password);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize Flyway: " + e.getMessage(), e);
        }

        try {
            return Flyway.configure()
                    .dataSource(databaseUrl, username, password)
                    .locations(flywayProperties.getLocations().toArray(new String[0]))
                    .baselineOnMigrate(true)
                    .schemas(databaseName)
                    .load();
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure Flyway: " + e.getMessage(), e);
        }
    }

    private void ensureDatabaseExists(String jdbcUrl, String databaseName, String username, String password) throws SQLException {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE DATABASE IF NOT EXISTS " + databaseName);
            }
        }
    }

    private void checkDatabaseConnection(String databaseUrl, String username, String password) throws SQLException {
        try (Connection connection = DriverManager.getConnection(databaseUrl, username, password)) {
            if (connection.isValid(5)) {
                System.out.println("Successfully connected to the database.");
            } else {
                throw new SQLException("Failed to establish a valid connection to the database.");
            }
        }
    }

    private String extractDatabaseNameFromUrl(String url) {
        return url.substring(url.lastIndexOf('/') + 1, url.contains("?") ? url.indexOf("?") : url.length());
    }

    private String extractJdbcUrlWithoutDatabaseName(String url) {
        return url.substring(0, url.lastIndexOf('/')) + "/";
    }
}
