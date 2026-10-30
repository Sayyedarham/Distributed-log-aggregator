package com.logaggregator.gateway.storage;

import com.logaggregator.gateway.model.AgentInfo;
import com.logaggregator.gateway.model.LogEntry;
import com.logaggregator.gateway.model.MetricPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public class PostgresWriter implements StorageWriter {

    private static final Logger logger = LoggerFactory.getLogger(PostgresWriter.class);

    private final ConnectionPool connectionPool;

    public PostgresWriter(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    @Override
    public String getName() {
        return "PostgresWriter";
    }

    @Override
    public void write(MetricPayload payload) {
        if (connectionPool == null || !connectionPool.isHealthy()) {
            return;
        }

        try (Connection conn = connectionPool.getConnection()) {

            AgentInfo info = payload.getAgentInfo();
            if (info != null) {
                upsertAgent(conn, info);
            }

            recordHeartbeat(conn, payload);

            recordAlerts(conn, payload);
        } catch (SQLException e) {
            logger.error("Failed writing metadata to PostgreSQL: {}", e.getMessage());
        }
    }

    private void upsertAgent(Connection conn, AgentInfo info) throws SQLException {
        String sql = "INSERT INTO agents (agent_id, hostname, ip_address, os_type, os_version, agent_version, boot_time, last_seen_at, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), 'active') " +
                "ON CONFLICT (agent_id) DO UPDATE SET " +
                "hostname = EXCLUDED.hostname, " +
                "ip_address = EXCLUDED.ip_address, " +
                "last_seen_at = NOW(), " +
                "status = 'active';";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, info.getAgentId());
            ps.setString(2, info.getHostname());
            ps.setString(3, info.getIpAddress());
            ps.setString(4, info.getOsType());
            ps.setString(5, info.getOsVersion());
            ps.setString(6, info.getAgentVersion());
            ps.setLong(7, info.getBootTime());
            ps.executeUpdate();
        }
    }

    private void recordHeartbeat(Connection conn, MetricPayload payload) throws SQLException {
        String sql = "INSERT INTO agent_heartbeats (agent_id, timestamp, metrics_count, sequence_number, latency_ms, status) " +
                "VALUES (?, ?, ?, ?, ?, ?);";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, payload.getAgentInfo() != null ? payload.getAgentInfo().getAgentId() : "unknown");
            ps.setTimestamp(2, new Timestamp(payload.getTimestamp()));
            ps.setInt(3, 1);
            ps.setLong(4, payload.getSequenceNumber());
            ps.setDouble(5, 0.0);
            ps.setString(6, "OK");
            ps.executeUpdate();
        }
    }

    private void recordAlerts(Connection conn, MetricPayload payload) throws SQLException {
        List<LogEntry> logs = payload.getLogEntries();
        if (logs == null || logs.isEmpty()) return;

        String sql = "INSERT INTO alerts (agent_id, alert_type, severity, message, metric_name, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?);";

        for (LogEntry entry : logs) {
            String level = entry.getLevel() != null ? entry.getLevel().toUpperCase() : "INFO";
            if ("ERROR".equals(level) || "FATAL".equals(level) || "CRITICAL".equals(level) || "WARN".equals(level)) {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, payload.getAgentInfo() != null ? payload.getAgentInfo().getAgentId() : "unknown");
                    ps.setString(2, "log_anomaly");
                    ps.setString(3, level.toLowerCase());
                    ps.setString(4, entry.getMessage());
                    ps.setString(5, entry.getSource());
                    ps.setTimestamp(6, new Timestamp(entry.getTimestamp()));
                    ps.executeUpdate();
                }
            }
        }
    }

    @Override
    public void flush() {}

    @Override
    public void close() {}
}

