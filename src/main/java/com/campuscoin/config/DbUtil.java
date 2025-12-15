package com.campuscoin.config;

import com.campuscoin.util.LogUtil;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

public final class DbUtil {
    private static final Logger logger = LogUtil.getLogger(DbUtil.class);
    private static String url;
    private static String username;
    private static String password;
    private static String driverClass;

    static {
        loadConfig();
        loadDriver();
    }

    private DbUtil() {
    }

    private static void loadConfig() {
        boolean loaded = loadFromYaml();
        if (!loaded) {
            loadFromProperties();
        }
    }

    public static Connection getConnection() throws SQLException {
        if (url == null || username == null || password == null) {
            throw new SQLException("Database configuration is missing");
        }
        return DriverManager.getConnection(url, username, password);
    }

    private static boolean loadFromYaml() {
        try (InputStream in = DbUtil.class.getClassLoader().getResourceAsStream("application.yml")) {
            if (in == null) {
                logger.info("application.yml not found, fallback to db.properties");
                return false;
            }
            Yaml yaml = new Yaml(new SafeConstructor());
            Object loaded = yaml.load(in);
            if (!(loaded instanceof Map)) {
                logger.warning("application.yml is not a valid map structure");
                return false;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> root = (Map<String, Object>) loaded;
            Map<String, Object> datasource = asMap(root.get("datasource"));
            if (datasource == null) {
                logger.warning("datasource section missing in application.yml");
                return false;
            }
            url = asString(datasource.get("url"));
            username = asString(datasource.get("username"));
            password = asString(datasource.get("password"));
            driverClass = asString(datasource.get("driver"));
            return url != null && username != null && password != null;
        } catch (Exception e) {
            logger.warning("Failed to load application.yml: " + e.getMessage());
            return false;
        }
    }

    private static void loadFromProperties() {
        Properties props = new Properties();
        try (InputStream in = DbUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                logger.severe("db.properties not found in classpath");
                return;
            }
            props.load(in);
            url = props.getProperty("db.url");
            username = props.getProperty("db.username");
            password = props.getProperty("db.password");
            driverClass = props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
        } catch (IOException e) {
            logger.severe("Failed to load db.properties: " + e.getMessage());
        }
    }

    private static void loadDriver() {
        String driver = driverClass != null ? driverClass : "com.mysql.cj.jdbc.Driver";
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            logger.severe("JDBC driver not found: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object obj) {
        return (obj instanceof Map) ? (Map<String, Object>) obj : null;
    }

    private static String asString(Object obj) {
        return obj != null ? String.valueOf(obj) : null;
    }
}
