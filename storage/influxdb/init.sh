#!/usr/bin/env bash
# ============================================================================
# InfluxDB Initialization Script
# Distributed Log Aggregator - Storage Layer
# ============================================================================
# This script initializes InfluxDB with the required organization, buckets,
# retention policies, API tokens, and downsampling tasks.
#
# Usage: Run after InfluxDB container starts, or mount as an init script.
# Environment Variables:
#   INFLUXDB_URL          - InfluxDB HTTP API URL (default: http://localhost:8086)
#   INFLUXDB_ADMIN_TOKEN  - Admin token for setup (default: my-super-secret-token)
#   INFLUXDB_ORG          - Organization name (default: log-aggregator)
#   INFLUXDB_ADMIN_USER   - Admin username (default: admin)
#   INFLUXDB_ADMIN_PASS   - Admin password (default: adminpassword123)
# ============================================================================

set -euo pipefail

# ---------------------------------------------------------------------------
# Configuration (override via environment variables)
# ---------------------------------------------------------------------------
INFLUXDB_URL="${INFLUXDB_URL:-http://localhost:8086}"
INFLUXDB_ADMIN_TOKEN="${INFLUXDB_ADMIN_TOKEN:-my-super-secret-token}"
INFLUXDB_ORG="${INFLUXDB_ORG:-log-aggregator}"
INFLUXDB_ADMIN_USER="${INFLUXDB_ADMIN_USER:-admin}"
INFLUXDB_ADMIN_PASS="${INFLUXDB_ADMIN_PASS:-adminpassword123}"

# Bucket configurations: name:retention_seconds
METRICS_BUCKET="metrics"
METRICS_RETENTION="2592000"          # 30 days in seconds

GATEWAY_BUCKET="gateway_metrics"
GATEWAY_RETENTION="604800"           # 7 days in seconds

DOWNSAMPLED_5M_BUCKET="metrics_5m"
DOWNSAMPLED_5M_RETENTION="7776000"   # 90 days in seconds

DOWNSAMPLED_1H_BUCKET="metrics_1h"
DOWNSAMPLED_1H_RETENTION="31536000"  # 365 days in seconds

MAX_RETRIES=30
RETRY_INTERVAL=2

# ---------------------------------------------------------------------------
# Utility Functions
# ---------------------------------------------------------------------------
log_info() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [INFO]  $*"
}

log_error() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [ERROR] $*" >&2
}

log_warn() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [WARN]  $*"
}

# ---------------------------------------------------------------------------
# Wait for InfluxDB to be ready
# ---------------------------------------------------------------------------
wait_for_influxdb() {
    log_info "Waiting for InfluxDB to be ready at ${INFLUXDB_URL}..."
    local retries=0

    while [ $retries -lt $MAX_RETRIES ]; do
        if curl -sf "${INFLUXDB_URL}/health" > /dev/null 2>&1; then
            log_info "InfluxDB is ready!"
            return 0
        fi

        retries=$((retries + 1))
        log_info "InfluxDB not ready yet (attempt ${retries}/${MAX_RETRIES}). Retrying in ${RETRY_INTERVAL}s..."
        sleep $RETRY_INTERVAL
    done

    log_error "InfluxDB failed to become ready after $((MAX_RETRIES * RETRY_INTERVAL)) seconds"
    exit 1
}

# ---------------------------------------------------------------------------
# Initial Setup (creates org, user, bucket)
# ---------------------------------------------------------------------------
setup_influxdb() {
    log_info "Running InfluxDB initial setup..."

    # Check if already set up
    local health_response
    health_response=$(curl -sf "${INFLUXDB_URL}/api/v2/setup" 2>/dev/null || true)

    if echo "${health_response}" | grep -q '"allowed":false'; then
        log_info "InfluxDB is already set up. Skipping initial setup."
        return 0
    fi

    # Perform initial setup
    local setup_response
    setup_response=$(curl -sf -X POST "${INFLUXDB_URL}/api/v2/setup" \
        -H "Content-Type: application/json" \
        -d '{
            "username": "'"${INFLUXDB_ADMIN_USER}"'",
            "password": "'"${INFLUXDB_ADMIN_PASS}"'",
            "org": "'"${INFLUXDB_ORG}"'",
            "bucket": "'"${METRICS_BUCKET}"'",
            "retentionPeriodSeconds": '"${METRICS_RETENTION}"',
            "token": "'"${INFLUXDB_ADMIN_TOKEN}"'"
        }' 2>&1)

    if [ $? -eq 0 ]; then
        log_info "InfluxDB initial setup completed successfully."
    else
        log_error "Failed to set up InfluxDB: ${setup_response}"
        exit 1
    fi
}

# ---------------------------------------------------------------------------
# Helper: Get Organization ID
# ---------------------------------------------------------------------------
get_org_id() {
    local org_id
    org_id=$(curl -sf "${INFLUXDB_URL}/api/v2/orgs" \
        -H "Authorization: Token ${INFLUXDB_ADMIN_TOKEN}" \
        | python3 -c "import sys,json; orgs=json.load(sys.stdin)['orgs']; print(next(o['id'] for o in orgs if o['name']=='${INFLUXDB_ORG}'))" 2>/dev/null \
        || curl -sf "${INFLUXDB_URL}/api/v2/orgs" \
            -H "Authorization: Token ${INFLUXDB_ADMIN_TOKEN}" \
            | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
    echo "${org_id}"
}

# ---------------------------------------------------------------------------
# Create a bucket if it doesn't exist
# ---------------------------------------------------------------------------
create_bucket() {
    local bucket_name="$1"
    local retention_seconds="$2"
    local org_id="$3"

    # Check if bucket exists
    local existing
    existing=$(curl -sf "${INFLUXDB_URL}/api/v2/buckets?name=${bucket_name}" \
        -H "Authorization: Token ${INFLUXDB_ADMIN_TOKEN}" \
        | grep -c "\"name\":\"${bucket_name}\"" || true)

    if [ "${existing}" -gt 0 ]; then
        log_info "Bucket '${bucket_name}' already exists. Skipping."
        return 0
    fi

    log_info "Creating bucket '${bucket_name}' with ${retention_seconds}s retention..."

    local response
    response=$(curl -sf -X POST "${INFLUXDB_URL}/api/v2/buckets" \
        -H "Authorization: Token ${INFLUXDB_ADMIN_TOKEN}" \
        -H "Content-Type: application/json" \
        -d '{
            "name": "'"${bucket_name}"'",
            "orgID": "'"${org_id}"'",
            "retentionRules": [
                {
                    "type": "expire",
                    "everySeconds": '"${retention_seconds}"'
                }
            ]
        }' 2>&1)

    if [ $? -eq 0 ]; then
        log_info "Bucket '${bucket_name}' created successfully."
    else
        log_error "Failed to create bucket '${bucket_name}': ${response}"
    fi
}

# ---------------------------------------------------------------------------
# Create Downsampling Tasks
# ---------------------------------------------------------------------------
create_downsampling_tasks() {
    local org_id="$1"

    log_info "Creating downsampling tasks..."

    # Task 1: Downsample from 'metrics' (raw) -> 'metrics_5m' (5-minute aggregates)
    local task_5m_flux='
option task = {name: "downsample_5m", every: 5m}

data = from(bucket: "metrics")
    |> range(start: -10m)
    |> filter(fn: (r) => r._measurement =~ /^(cpu_metrics|memory_metrics|disk_metrics|network_metrics)$/)

data
    |> aggregateWindow(every: 5m, fn: mean, createEmpty: false)
    |> to(bucket: "metrics_5m", org: "'"${INFLUXDB_ORG}"'")
'

    curl -sf -X POST "${INFLUXDB_URL}/api/v2/tasks" \
        -H "Authorization: Token ${INFLUXDB_ADMIN_TOKEN}" \
        -H "Content-Type: application/json" \
        -d '{
            "orgID": "'"${org_id}"'",
            "flux": "'"$(echo "${task_5m_flux}" | sed ':a;N;$!ba;s/\n/\\n/g' | sed 's/"/\\"/g')"'",
            "status": "active",
            "description": "Downsample raw metrics to 5-minute aggregates"
        }' > /dev/null 2>&1 && \
        log_info "Created downsampling task: 5-minute aggregates" || \
        log_warn "Could not create 5-minute downsampling task (may already exist)"

    # Task 2: Downsample from 'metrics_5m' -> 'metrics_1h' (1-hour aggregates)
    local task_1h_flux='
option task = {name: "downsample_1h", every: 1h}

data = from(bucket: "metrics_5m")
    |> range(start: -2h)
    |> filter(fn: (r) => r._measurement =~ /^(cpu_metrics|memory_metrics|disk_metrics|network_metrics)$/)

data
    |> aggregateWindow(every: 1h, fn: mean, createEmpty: false)
    |> to(bucket: "metrics_1h", org: "'"${INFLUXDB_ORG}"'")
'

    curl -sf -X POST "${INFLUXDB_URL}/api/v2/tasks" \
        -H "Authorization: Token ${INFLUXDB_ADMIN_TOKEN}" \
        -H "Content-Type: application/json" \
        -d '{
            "orgID": "'"${org_id}"'",
            "flux": "'"$(echo "${task_1h_flux}" | sed ':a;N;$!ba;s/\n/\\n/g' | sed 's/"/\\"/g')"'",
            "status": "active",
            "description": "Downsample 5-minute metrics to 1-hour aggregates"
        }' > /dev/null 2>&1 && \
        log_info "Created downsampling task: 1-hour aggregates" || \
        log_warn "Could not create 1-hour downsampling task (may already exist)"
}

# ---------------------------------------------------------------------------
# Create Read-Only API Token for Grafana
# ---------------------------------------------------------------------------
create_grafana_token() {
    local org_id="$1"

    log_info "Creating Grafana read-only API token..."

    # Note: In production, this token should be stored securely.
    # The admin token is used by default in the datasource config.
    # This creates an additional scoped token for Grafana if needed.

    curl -sf -X POST "${INFLUXDB_URL}/api/v2/authorizations" \
        -H "Authorization: Token ${INFLUXDB_ADMIN_TOKEN}" \
        -H "Content-Type: application/json" \
        -d '{
            "orgID": "'"${org_id}"'",
            "description": "Grafana read-only token",
            "permissions": [
                {"action": "read", "resource": {"type": "buckets", "orgID": "'"${org_id}"'"}},
                {"action": "read", "resource": {"type": "tasks", "orgID": "'"${org_id}"'"}}
            ]
        }' > /dev/null 2>&1 && \
        log_info "Grafana read-only token created." || \
        log_warn "Could not create Grafana token (may already exist)"
}

# ---------------------------------------------------------------------------
# Main Execution
# ---------------------------------------------------------------------------
main() {
    log_info "============================================"
    log_info " InfluxDB Initialization"
    log_info " Distributed Log Aggregator"
    log_info "============================================"

    # Step 1: Wait for InfluxDB
    wait_for_influxdb

    # Step 2: Run initial setup (creates org, admin user, initial bucket)
    setup_influxdb

    # Step 3: Get organization ID for subsequent API calls
    local org_id
    org_id=$(get_org_id)
    if [ -z "${org_id}" ]; then
        log_error "Could not retrieve organization ID. Aborting."
        exit 1
    fi
    log_info "Organization ID: ${org_id}"

    # Step 4: Create additional buckets
    create_bucket "${GATEWAY_BUCKET}"       "${GATEWAY_RETENTION}"       "${org_id}"
    create_bucket "${DOWNSAMPLED_5M_BUCKET}" "${DOWNSAMPLED_5M_RETENTION}" "${org_id}"
    create_bucket "${DOWNSAMPLED_1H_BUCKET}" "${DOWNSAMPLED_1H_RETENTION}" "${org_id}"

    # Step 5: Create downsampling tasks
    create_downsampling_tasks "${org_id}"

    # Step 6: Create Grafana read-only token
    create_grafana_token "${org_id}"

    log_info "============================================"
    log_info " InfluxDB initialization complete!"
    log_info "============================================"
    log_info "Buckets created:"
    log_info "  - ${METRICS_BUCKET} (${METRICS_RETENTION}s retention)"
    log_info "  - ${GATEWAY_BUCKET} (${GATEWAY_RETENTION}s retention)"
    log_info "  - ${DOWNSAMPLED_5M_BUCKET} (${DOWNSAMPLED_5M_RETENTION}s retention)"
    log_info "  - ${DOWNSAMPLED_1H_BUCKET} (${DOWNSAMPLED_1H_RETENTION}s retention)"
    log_info "Downsampling: raw -> 5m -> 1h"
}

main "$@"
