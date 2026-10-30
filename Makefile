# =============================================================================
# Distributed Log Aggregator & Metric Monitor — Makefile
# =============================================================================

.PHONY: proto build-agent build-gateway build-all run stop clean test help

.DEFAULT_GOAL := help

# ---------------------------------------------------------------------------
# Proto — Generate Protobuf code for Python and Java
# ---------------------------------------------------------------------------
proto:
	@echo "=== Generating Protobuf code ==="
ifeq ($(OS),Windows_NT)
	@scripts\generate_proto.bat
else
	@chmod +x scripts/generate_proto.sh && ./scripts/generate_proto.sh
endif
	@echo "=== Protobuf generation complete ==="

# ---------------------------------------------------------------------------
# Build — Edge Agent (Python)
# ---------------------------------------------------------------------------
build-agent:
	@echo "=== Building Edge Agent ==="
	cd edge-agent && pip install -r requirements.txt
	@echo "=== Edge Agent build complete ==="

# ---------------------------------------------------------------------------
# Build — Ingestion Gateway (Java / Maven)
# ---------------------------------------------------------------------------
build-gateway:
	@echo "=== Building Ingestion Gateway ==="
	cd ingestion-gateway && mvn clean package -DskipTests
	@echo "=== Ingestion Gateway build complete ==="

# ---------------------------------------------------------------------------
# Build — All components
# ---------------------------------------------------------------------------
build-all: build-agent build-gateway
	@echo "=== All components built successfully ==="

# ---------------------------------------------------------------------------
# Run — Start all services via Docker Compose
# ---------------------------------------------------------------------------
run:
	@echo "=== Starting all services via Docker Compose ==="
	docker-compose up --build -d
	@echo "=== All services started ==="
	@echo "  Grafana:          http://localhost:3000  (admin/admin)"
	@echo "  Ingestion Gateway: localhost:9090 (TCP Netty)"
	@echo "  InfluxDB:         http://localhost:8086"
	@echo "  PostgreSQL:       localhost:5432"

# ---------------------------------------------------------------------------
# Stop — Stop all Docker Compose services
# ---------------------------------------------------------------------------
stop:
	@echo "=== Stopping all services ==="
	docker-compose down
	@echo "=== All services stopped ==="

# ---------------------------------------------------------------------------
# Clean — Remove build artifacts and caches
# ---------------------------------------------------------------------------
clean:
	@echo "=== Cleaning build artifacts ==="
	rm -rf edge-agent/build/ edge-agent/dist/ edge-agent/*.egg-info
	find edge-agent -type d -name __pycache__ -exec rm -rf {} + 2>/dev/null || true
	rm -rf ingestion-gateway/target/
	@echo "=== Clean complete ==="

# ---------------------------------------------------------------------------
# Test — Run all tests across all components
# ---------------------------------------------------------------------------
test:
	@echo "=== Running Edge Agent tests (Python) ==="
	cd edge-agent && python -m unittest discover -s tests -v
	@echo ""
	@echo "=== Running Ingestion Gateway tests (Java / Maven) ==="
	cd ingestion-gateway && mvn test
	@echo ""
	@echo "=== All tests complete ==="

# ---------------------------------------------------------------------------
# Help — Show available targets
# ---------------------------------------------------------------------------
help:
	@echo ""
	@echo "Distributed Log Aggregator & Metric Monitor — Targets"
	@echo "======================================================"
	@echo "  make proto           Generate Protobuf stubs"
	@echo "  make build-agent     Install edge agent dependencies"
	@echo "  make build-gateway   Build Java Netty gateway package"
	@echo "  make build-all       Build all tiers"
	@echo "  make run             Start full 3-tier cluster via Docker Compose"
	@echo "  make stop            Stop Docker Compose services"
	@echo "  make clean           Clean build artifacts"
	@echo "  make test            Execute test suites across tiers"
	@echo "  make help            Show this help"
	@echo ""
