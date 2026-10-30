SELECT 'CREATE DATABASE log_aggregator'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'log_aggregator')\gexec
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'aggregator') THEN
        CREATE ROLE aggregator WITH LOGIN PASSWORD 'aggregator_pass';
    END IF;
END
$$;
\connect log_aggregator;
GRANT USAGE ON SCHEMA public TO aggregator;
GRANT CREATE ON SCHEMA public TO aggregator;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO aggregator;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO aggregator;
\echo '=== Running Migration 001: Create Tables ==='
\i /docker-entrypoint-initdb.d/migrations/001_create_tables.sql
\echo '=== Running Migration 002: Create Indexes ==='
\i /docker-entrypoint-initdb.d/migrations/002_create_indexes.sql
\echo '=== Running Migration 003: Create Views ==='
\i /docker-entrypoint-initdb.d/migrations/003_create_views.sql
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO aggregator;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO aggregator;
\echo '=== Database initialization complete ==='
