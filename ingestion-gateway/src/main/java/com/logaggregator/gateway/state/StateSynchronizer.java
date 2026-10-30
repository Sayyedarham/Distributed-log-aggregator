package com.logaggregator.gateway.state;

import com.logaggregator.gateway.storage.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StateSynchronizer {

    private static final Logger logger = LoggerFactory.getLogger(StateSynchronizer.class);

    private final AgentRegistry registry;
    private final ConnectionPool connectionPool;
    private final ScheduledExecutorService scheduler;

    public StateSynchronizer(AgentRegistry registry, ConnectionPool connectionPool) {
        this.registry = registry;
        this.connectionPool = connectionPool;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "state-synchronizer");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::syncState, 15, 30, TimeUnit.SECONDS);
        logger.info("StateSynchronizer background task scheduled every 30 seconds.");
    }

    private void syncState() {
        if (connectionPool == null || !connectionPool.isHealthy()) {
            return;
        }

        try (Connection conn = connectionPool.getConnection()) {
            String sql = "UPDATE agents SET last_seen_at = TO_TIMESTAMP(? / 1000.0), status = ? WHERE agent_id = ?;";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (AgentState state : registry.getAllAgents()) {
                    ps.setLong(1, state.getLastHeartbeatTime());
                    ps.setString(2, state.isActive() ? "active" : "inactive");
                    ps.setString(3, state.getAgentInfo().getAgentId());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        } catch (Exception e) {
            logger.debug("State synchronization skipped or deferred: {}", e.getMessage());
        }
    }

    public void stop() {
        scheduler.shutdown();
    }
}

