#!/usr/bin/env bash
# =============================================================================
# Distributed Log Aggregator — Protobuf Code Generation (Linux / macOS)
# =============================================================================
# Compiles .proto files into Python and Java source code.
#
# Prerequisites:
#   - protoc (Protocol Buffers compiler) installed and on PATH
#   - grpcio-tools Python package (pip install grpcio-tools)
#
# Usage:
#   ./scripts/generate_proto.sh
# =============================================================================

set -euo pipefail

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
PROTO_DIR="${PROJECT_ROOT}/proto"
PROTO_FILE="${PROTO_DIR}/metrics.proto"

# Output directories
PYTHON_OUT="${PROJECT_ROOT}/edge-agent/src/generated"
JAVA_OUT="${PROJECT_ROOT}/ingestion-gateway/src/main/java"

# ---------------------------------------------------------------------------
# Validation
# ---------------------------------------------------------------------------
echo "[proto] Validating environment..."

if ! command -v protoc &>/dev/null; then
    echo "[ERROR] 'protoc' is not installed or not on PATH."
    echo "        Install from: https://github.com/protocolbuffers/protobuf/releases"
    exit 1
fi

if ! python3 -c "import grpc_tools" &>/dev/null; then
    echo "[WARN] grpcio-tools not found. Installing..."
    pip install grpcio-tools
fi

if [[ ! -f "${PROTO_FILE}" ]]; then
    echo "[ERROR] Proto file not found: ${PROTO_FILE}"
    exit 1
fi

echo "[proto] Using protoc $(protoc --version)"

# ---------------------------------------------------------------------------
# Python Code Generation
# ---------------------------------------------------------------------------
echo "[proto] Generating Python code..."

mkdir -p "${PYTHON_OUT}"

python3 -m grpc_tools.protoc \
    --proto_path="${PROTO_DIR}" \
    --python_out="${PYTHON_OUT}" \
    --pyi_out="${PYTHON_OUT}" \
    --grpc_python_out="${PYTHON_OUT}" \
    "${PROTO_FILE}"

# Create __init__.py so the generated module is importable
touch "${PYTHON_OUT}/__init__.py"

echo "[proto] Python code generated in: ${PYTHON_OUT}"

# ---------------------------------------------------------------------------
# Java Code Generation
# ---------------------------------------------------------------------------
echo "[proto] Generating Java code..."

mkdir -p "${JAVA_OUT}"

protoc \
    --proto_path="${PROTO_DIR}" \
    --java_out="${JAVA_OUT}" \
    "${PROTO_FILE}"

echo "[proto] Java code generated in: ${JAVA_OUT}"

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
echo ""
echo "[proto] ✅ Code generation complete!"
echo "[proto]    Python : ${PYTHON_OUT}"
echo "[proto]    Java   : ${JAVA_OUT}"
echo ""
echo "[proto] Generated files:"
find "${PYTHON_OUT}" -name "*.py" -o -name "*.pyi" 2>/dev/null | sed 's/^/  /'
find "${JAVA_OUT}" -path "*/logaggregator/proto/*" -name "*.java" 2>/dev/null | sed 's/^/  /'
