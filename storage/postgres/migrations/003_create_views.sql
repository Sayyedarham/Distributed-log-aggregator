BEGIN;
CREATE OR REPLACE VIEW v_active_agents AS
SELECT
    a.agent_id,
    a.hostname,
    a.ip_address,
    a.os_type,
    a.os_version,
    a.agent_version,
    a.registered_at,
    a.last_seen_at,
    a.collection_interval_ms,
    a.config,
    h.latest_heartbeat,
    h.latest_latency_ms,
    h.latest_metrics_count,
    h.latest_sequence_number,
    EXTRACT(EPOCH FROM (NOW() - COALESCE(h.latest_heartbeat, a.last_seen_at))) AS seconds_since_heartbeat,
    CASE
        WHEN a.status = 'deregistered' THEN 'deregistered'
        WHEN h.latest_heartbeat IS NULL THEN 'unknown'
        WHEN EXTRACT(EPOCH FROM (NOW() - h.latest_heartbeat)) > (a.collection_interval_ms / 1000.0 * 3) THEN 'inactive'
        ELSE 'active'
    END AS effective_status
FROM agents a
LEFT JOIN LATERAL (
    SELECT
        ah.timestamp      AS latest_heartbeat,
        ah.latency_ms     AS latest_latency_ms,
        ah.metrics_count  AS latest_metrics_count,
        ah.sequence_number AS latest_sequence_number
    FROM agent_heartbeats ah
    WHERE ah.agent_id = a.agent_id
    ORDER BY ah.timestamp DESC
    LIMIT 1
) h ON TRUE
WHERE a.status != 'deregistered';
COMMENT ON VIEW v_active_agents IS 'Active agents enriched with latest heartbeat data and computed staleness';
CREATE OR REPLACE VIEW v_unresolved_alerts AS
SELECT
    al.id AS alert_id,
    al.agent_id,
    a.hostname,
    a.ip_address,
    al.alert_type,
    al.severity,
    al.message,
    al.metric_name,
    al.metric_value,
    al.threshold_value,
    al.created_at,
    al.acknowledged_at,
    al.acknowledged_by,
    EXTRACT(EPOCH FROM (NOW() - al.created_at)) AS open_duration_seconds,
    CASE
        WHEN al.acknowledged_at IS NOT NULL THEN 'acknowledged'
        ELSE 'new'
    END AS alert_state
FROM alerts al
LEFT JOIN agents a ON al.agent_id = a.agent_id
WHERE al.resolved_at IS NULL
ORDER BY
    CASE al.severity
        WHEN 'fatal'    THEN 1
        WHEN 'critical' THEN 2
        WHEN 'warning'  THEN 3
        WHEN 'info'     THEN 4
        ELSE 5
    END,
    al.created_at DESC;
COMMENT ON VIEW v_unresolved_alerts IS 'Unresolved alerts with agent details, ordered by severity and recency';
CREATE OR REPLACE VIEW v_agent_summary AS
SELECT
    a.agent_id,
    a.hostname,
    a.ip_address,
    a.os_type,
    a.os_version,
    a.agent_version,
    a.status,
    a.registered_at,
    a.last_seen_at,
    a.collection_interval_ms,
    COALESCE(hs.heartbeat_count_24h, 0) AS heartbeat_count_24h,
    hs.avg_latency_ms_24h,
    hs.max_latency_ms_24h,
    hs.last_heartbeat_at,
    COALESCE(als.total_alerts, 0) AS total_alerts,
    COALESCE(als.unresolved_alerts, 0) AS unresolved_alerts,
    COALESCE(als.critical_alerts, 0) AS critical_unresolved_alerts,
    als.latest_alert_at,
    COALESCE(ps.batches_processed_24h, 0) AS batches_processed_24h,
    COALESCE(ps.pipeline_errors_24h, 0) AS pipeline_errors_24h,
    ps.avg_pipeline_duration_ms_24h
FROM agents a
LEFT JOIN LATERAL (
    SELECT
        COUNT(*)                    AS heartbeat_count_24h,
        ROUND(AVG(latency_ms)::numeric, 2)  AS avg_latency_ms_24h,
        ROUND(MAX(latency_ms)::numeric, 2)  AS max_latency_ms_24h,
        MAX(timestamp)              AS last_heartbeat_at
    FROM agent_heartbeats
    WHERE agent_id = a.agent_id
      AND timestamp > NOW() - INTERVAL '24 hours'
) hs ON TRUE
LEFT JOIN LATERAL (
    SELECT
        COUNT(*)                                                    AS total_alerts,
        COUNT(*) FILTER (WHERE resolved_at IS NULL)                 AS unresolved_alerts,
        COUNT(*) FILTER (WHERE resolved_at IS NULL AND severity IN ('critical', 'fatal')) AS critical_alerts,
        MAX(created_at)                                             AS latest_alert_at
    FROM alerts
    WHERE agent_id = a.agent_id
) als ON TRUE
LEFT JOIN LATERAL (
    SELECT
        COUNT(DISTINCT batch_id)                                    AS batches_processed_24h,
        COUNT(*) FILTER (WHERE status = 'error')                    AS pipeline_errors_24h,
        ROUND(AVG(duration_ms)::numeric, 2)                         AS avg_pipeline_duration_ms_24h
    FROM pipeline_log
    WHERE agent_id = a.agent_id
      AND processed_at > NOW() - INTERVAL '24 hours'
) ps ON TRUE;
COMMENT ON VIEW v_agent_summary IS 'Comprehensive per-agent summary with heartbeat, alert, and pipeline statistics';
CREATE OR REPLACE VIEW v_pipeline_stats AS
SELECT
    stage_name,
    status,
    COUNT(*)                                            AS total_executions,
    ROUND(AVG(duration_ms)::numeric, 2)                 AS avg_duration_ms,
    ROUND(PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY duration_ms)::numeric, 2) AS p50_duration_ms,
    ROUND(PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY duration_ms)::numeric, 2) AS p95_duration_ms,
    ROUND(PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY duration_ms)::numeric, 2) AS p99_duration_ms,
    ROUND(MIN(duration_ms)::numeric, 2)                 AS min_duration_ms,
    ROUND(MAX(duration_ms)::numeric, 2)                 AS max_duration_ms,
    ROUND(STDDEV(duration_ms)::numeric, 2)              AS stddev_duration_ms,
    MIN(processed_at)                                   AS first_seen,
    MAX(processed_at)                                   AS last_seen
FROM pipeline_log
WHERE processed_at > NOW() - INTERVAL '24 hours'
GROUP BY stage_name, status
ORDER BY stage_name, status;
COMMENT ON VIEW v_pipeline_stats IS 'Pipeline stage performance statistics with percentile latencies (last 24h)';
CREATE OR REPLACE VIEW v_pipeline_hourly AS
SELECT
    date_trunc('hour', processed_at) AS hour,
    stage_name,
    COUNT(*)                                           AS total_count,
    COUNT(*) FILTER (WHERE status = 'success')         AS success_count,
    COUNT(*) FILTER (WHERE status = 'error')           AS error_count,
    COUNT(*) FILTER (WHERE status = 'skipped')         AS skipped_count,
    ROUND(AVG(duration_ms)::numeric, 2)                AS avg_duration_ms,
    ROUND(
        (COUNT(*) FILTER (WHERE status = 'error'))::numeric /
        NULLIF(COUNT(*), 0) * 100, 2
    ) AS error_rate_pct
FROM pipeline_log
WHERE processed_at > NOW() - INTERVAL '7 days'
GROUP BY date_trunc('hour', processed_at), stage_name
ORDER BY hour DESC, stage_name;
COMMENT ON VIEW v_pipeline_hourly IS 'Hourly pipeline throughput and error rates per stage (last 7 days)';
CREATE OR REPLACE VIEW v_alert_summary AS
SELECT
    alert_type,
    severity,
    COUNT(*)                                                    AS total_count,
    COUNT(*) FILTER (WHERE resolved_at IS NULL)                 AS unresolved_count,
    COUNT(*) FILTER (WHERE acknowledged_at IS NOT NULL AND resolved_at IS NULL) AS acknowledged_count,
    COUNT(*) FILTER (WHERE resolved_at IS NOT NULL)             AS resolved_count,
    ROUND(AVG(
        EXTRACT(EPOCH FROM (COALESCE(resolved_at, NOW()) - created_at))
    )::numeric, 0) AS avg_open_duration_seconds,
    MIN(created_at) AS first_alert,
    MAX(created_at) AS latest_alert
FROM alerts
GROUP BY alert_type, severity
ORDER BY
    CASE severity
        WHEN 'fatal'    THEN 1
        WHEN 'critical' THEN 2
        WHEN 'warning'  THEN 3
        WHEN 'info'     THEN 4
    END,
    alert_type;
COMMENT ON VIEW v_alert_summary IS 'Alert statistics grouped by type and severity with resolution metrics';
COMMIT;
