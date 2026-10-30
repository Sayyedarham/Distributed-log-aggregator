BEGIN;
CREATE INDEX IF NOT EXISTS idx_heartbeats_agent_timestamp
    ON agent_heartbeats (agent_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_heartbeats_timestamp
    ON agent_heartbeats (timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_alerts_agent_created
    ON alerts (agent_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_alerts_severity_created
    ON alerts (severity, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_alerts_type
    ON alerts (alert_type);
CREATE INDEX IF NOT EXISTS idx_alerts_unresolved
    ON alerts (created_at DESC)
    WHERE resolved_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_alerts_unacknowledged
    ON alerts (severity, created_at DESC)
    WHERE acknowledged_at IS NULL AND resolved_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_pipeline_batch
    ON pipeline_log (batch_id);
CREATE INDEX IF NOT EXISTS idx_pipeline_agent_processed
    ON pipeline_log (agent_id, processed_at DESC);
CREATE INDEX IF NOT EXISTS idx_pipeline_stage_status
    ON pipeline_log (stage_name, status, processed_at DESC);
CREATE INDEX IF NOT EXISTS idx_pipeline_errors
    ON pipeline_log (processed_at DESC)
    WHERE status = 'error';
CREATE INDEX IF NOT EXISTS idx_gateway_status
    ON gateway_instances (status);
CREATE INDEX IF NOT EXISTS idx_gateway_heartbeat
    ON gateway_instances (last_heartbeat DESC);
CREATE INDEX IF NOT EXISTS idx_agents_status
    ON agents (status);
CREATE INDEX IF NOT EXISTS idx_agents_last_seen
    ON agents (last_seen_at DESC);
COMMIT;
