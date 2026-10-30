package com.logaggregator.gateway.storage;

import com.logaggregator.gateway.config.GatewayConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionPool {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionPool.class);

    private final HikariDataSource dataSource;

    public ConnectionPool(GatewayConfig.PostgresConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s",
                config.getHost(), config.getPort(), config.getDatabase());
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setMaximumPoolSize(config.getMaxPoolSize());
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setIdleTimeout(30000);
        hikariConfig.setConnectionTimeout(5000);
        hikariConfig.setPoolName("Aggregator-HikariPool");

        HikariDataSource ds = null;
        try {
            ds = new HikariDataSource(hikariConfig);
            logger.info("Initialized PostgreSQL HikariCP connection pool for {}", jdbcUrl);
        } catch (Exception e) {
            logger.warn("PostgreSQL connection pool initialization failed (DB may not be ready): {}", e.getMessage());
        }
        this.dataSource = ds;
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("HikariDataSource is not initialized");
        }
        return dataSource.getConnection();
    }

    public boolean isHealthy() {
        if (dataSource == null) return false;
        try (Connection c = getConnection()) {
            return c.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Closed PostgreSQL HikariCP connection pool.");
        }
    }
}

