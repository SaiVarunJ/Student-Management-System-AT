package com.airtripe.studentmanagement.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConfigSingleton {
    private static final Logger logger = LoggerFactory.getLogger(ConfigSingleton.class);
    private static ConfigSingleton instance;

    private final String jdbcUrl;
    private final String jdbcUser;
    private final String jdbcPassword;
    private final String dataFilePath;

    private ConfigSingleton() {
        // read configuration from system properties or environment with sensible defaults
        this.jdbcUrl = System.getProperty("sms.jdbc.url", System.getenv().getOrDefault("SMS_JDBC_URL", "jdbc:h2:mem:sms;DB_CLOSE_DELAY=-1"));
        this.jdbcUser = System.getProperty("sms.jdbc.user", System.getenv().getOrDefault("SMS_JDBC_USER", "sa"));
        this.jdbcPassword = System.getProperty("sms.jdbc.password", System.getenv().getOrDefault("SMS_JDBC_PASSWORD", ""));
        // default data file in target directory so tests don't pollute project root
        this.dataFilePath = System.getProperty("sms.data.file", System.getenv().getOrDefault("SMS_DATA_FILE", "target/students.json"));
        logger.info("ConfigSingleton initialized with jdbcUrl={} dataFile={}", jdbcUrl, dataFilePath);
    }

    public static synchronized ConfigSingleton getInstance() {
        if (instance == null) instance = new ConfigSingleton();
        return instance;
    }

    public Connection getConnection() throws SQLException {
        // Create a new connection per call; callers are responsible for closing it
        logger.debug("Opening DB connection to {}", jdbcUrl);
        Properties props = new Properties();
        props.setProperty("user", jdbcUser);
        props.setProperty("password", jdbcPassword);
        return DriverManager.getConnection(jdbcUrl, props);
    }

    // getters for tests or other components
    public String getJdbcUrl() { return jdbcUrl; }
    public String getJdbcUser() { return jdbcUser; }
    public String getJdbcPassword() { return jdbcPassword; }
    public String getDataFilePath() { return dataFilePath; }
}
