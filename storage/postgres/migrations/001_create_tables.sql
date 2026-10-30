BEGIN;
CREATE TABLE IF NOT EXISTS agents (
    agent_id            VARCHAR(64)     PRIMARY KEY,
    hostname            VARCHAR(255)    NOT NULL,
    ip_address          VARCHAR(45)     NOT NULL,
    os_type             VARCHAR(20)     NOT NULL,
    os_version          VARCHAR(100),
    agent_version       VARCHAR(20),
    boot_time           BIGINT,
    registered_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_seen_at        TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    status              VARCHAR(20)     DEFAULT 'active',
    collection_interval_ms INTEGER      DEFAULT 10000,
    config              JSONB           DEFAULT '{}'::jsonb
);
COMMENT ON TABLE agents IS 'Registry of all monitoring agents connected to the aggregator';
COMMENT ON COLUMN agents.agent_id IS 'Unique identifier for the agent (typically hostname-based hash)';
COMMENT ON COLUMN agents.status IS 'Agent lifecycle status: active, inactive, or deregistered';
COMMENT ON COLUMN agents.config IS 'Agent-specific configuration overrides as JSON';
CREATE TABLE IF NOT EXISTS agent_heartbeats (
    id                  BIGSERIAL       PRIMARY KEY,
    agent_id            VARCHAR(64)     REFERENCES agents(agent_id) ON DELETE CASCADE,
    timestamp           TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    metrics_count       INTEGER,
    sequence_number     INTEGER,
    latency_ms          DOUBLE PRECISION,
    status              VARCHAR(20)
);
COMMENT ON TABLE agent_heartbeats IS 'Heartbeat records received from monitoring agents';
COMMENT ON COLUMN agent_heartbeats.latency_ms IS 'Measured network latency for this heartbeat in milliseconds';
CREATE TABLE IF NOT EXISTS alerts (
    id                  BIGSERIAL       PRIMARY KEY,
    agent_id            VARCHAR(64)     REFERENCES agents(agent_id) ON DELETE SET NULL,
    alert_type          VARCHAR(50)     NOT NULL,
    severity            VARCHAR(20)     NOT NULL,
    message             TEXT            NOT NULL,
    metric_name         VARCHAR(100),
    metric_value        DOUBLE PRECISION,
    threshold_value     DOUBLE PRECISION,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    acknowledged_at     TIMESTAMP WITH TIME ZONE,
    resolved_at         TIMESTAMP WITH TIME ZONE,
    acknowledged_by     VARCHAR(100)
);
COMMENT ON TABLE alerts IS 'System alerts generated when metrics cross configured thresholds';
COMMENT ON COLUMN alerts.alert_type IS 'Category of alert: cpu_high, memory_critical, disk_full, network_saturated, agent_offline';
COMMENT ON COLUMN alerts.severity IS 'Alert severity level: info, warning, critical, fatal';
CREATE TABLE IF NOT EXISTS pipeline_log (
    id                  BIGSERIAL       PRIMARY KEY,
    batch_id            VARCHAR(64),
    agent_id            VARCHAR(64),
    stage_name          VARCHAR(50),
    status              VARCHAR(20),
    duration_ms         DOUBLE PRECISION,
    error_message       TEXT,
    processed_at        TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
COMMENT ON TABLE pipeline_log IS 'Audit trail of metric batch processing through pipeline stages';
COMMENT ON COLUMN pipeline_log.stage_name IS 'Pipeline stage: ingest, validate, transform, store, alert';
CREATE TABLE IF NOT EXISTS gateway_instances (
    instance_id         VARCHAR(64)     PRIMARY KEY,
    host                VARCHAR(255),
    port                INTEGER,
    started_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_heartbeat      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    status              VARCHAR(20)     DEFAULT 'active',
    metrics             JSONB           DEFAULT '{}'::jsonb
);
COMMENT ON TABLE gateway_instances IS 'Registry of active gateway server instances';
COMMENT ON COLUMN gateway_instances.metrics IS 'Latest runtime metrics snapshot (connections, throughput, etc.)';
CREATE TABLE IF NOT EXISTS metric_metadata (
    metric_name         VARCHAR(100)    PRIMARY KEY,
    metric_type         VARCHAR(20)     NOT NULL,
    unit                VARCHAR(50),
    description         TEXT,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
COMMENT ON TABLE metric_metadata IS 'Catalog of available metric types and their descriptions';
INSERT INTO metric_metadata (metric_name, metric_type, unit, description) VALUES
    ('cpu.user_percent',        'gauge', 'percent',   'CPU time spent in user mode'),
    ('cpu.system_percent',      'gauge', 'percent',   'CPU time spent in kernel mode'),
    ('cpu.idle_percent',        'gauge', 'percent',   'CPU idle time'),
    ('cpu.iowait_percent',      'gauge', 'percent',   'CPU time waiting for I/O'),
    ('cpu.load_avg_1m',         'gauge', 'load',      '1-minute load average'),
    ('cpu.load_avg_5m',         'gauge', 'load',      '5-minute load average'),
    ('cpu.load_avg_15m',        'gauge', 'load',      '15-minute load average'),
    ('memory.used_percent',     'gauge', 'percent',   'Memory utilization'),
    ('memory.total_bytes',      'gauge', 'bytes',     'Total physical memory'),
    ('memory.available_bytes',  'gauge', 'bytes',     'Available memory'),
    ('memory.swap_used_bytes',  'gauge', 'bytes',     'Swap space used'),
    ('disk.read_rate_bps',      'gauge', 'bytes/sec', 'Disk read throughput'),
    ('disk.write_rate_bps',     'gauge', 'bytes/sec', 'Disk write throughput'),
    ('disk.utilization_percent','gauge', 'percent',   'Disk I/O utilization'),
    ('disk.used_percent',       'gauge', 'percent',   'Disk space used'),
    ('network.send_rate_bps',   'gauge', 'bytes/sec', 'Network send rate'),
    ('network.recv_rate_bps',   'gauge', 'bytes/sec', 'Network receive rate')
ON CONFLICT (metric_name) DO NOTHING;
COMMIT;
