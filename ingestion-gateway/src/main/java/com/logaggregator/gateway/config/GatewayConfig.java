package com.logaggregator.gateway.config;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GatewayConfig {

    private ServerConfig server = new ServerConfig();
    private StorageConfig storage = new StorageConfig();
    private PipelineConfig pipeline = new PipelineConfig();
    private LoggingConfig logging = new LoggingConfig();

    public static GatewayConfig load(String path) {
        Yaml yaml = new Yaml();
        GatewayConfig config = new GatewayConfig();

        InputStream is = null;
        try {
            if (path != null && new File(path).exists()) {
                is = new FileInputStream(path);
            } else {
                is = GatewayConfig.class.getClassLoader().getResourceAsStream("gateway.yaml");
            }

            if (is != null) {
                Map<String, Object> data = yaml.load(is);
                if (data != null) {
                    config.populate(data);
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not load configuration from " + path + ": " + e.getMessage() + ". Using defaults.");
        } finally {
            if (is != null) {
                try { is.close(); } catch (Exception ignored) {}
            }
        }

        config.applyEnvOverrides();
        return config;
    }

    @SuppressWarnings("unchecked")
    private void populate(Map<String, Object> data) {
        if (data.containsKey("server")) {
            Map<String, Object> s = (Map<String, Object>) data.get("server");
            if (s.containsKey("host")) server.setHost((String) s.get("host"));
            if (s.containsKey("port")) server.setPort(((Number) s.get("port")).intValue());
            if (s.containsKey("boss_threads")) server.setBossThreads(((Number) s.get("boss_threads")).intValue());
            if (s.containsKey("worker_threads")) server.setWorkerThreads(((Number) s.get("worker_threads")).intValue());
            if (s.containsKey("max_frame_size_bytes")) server.setMaxFrameSizeBytes(((Number) s.get("max_frame_size_bytes")).intValue());
        }

        if (data.containsKey("storage")) {
            Map<String, Object> st = (Map<String, Object>) data.get("storage");
            if (st.containsKey("influxdb")) {
                Map<String, Object> inf = (Map<String, Object>) st.get("influxdb");
                if (inf.containsKey("url")) storage.getInfluxdb().setUrl((String) inf.get("url"));
                if (inf.containsKey("token")) storage.getInfluxdb().setToken((String) inf.get("token"));
                if (inf.containsKey("org")) storage.getInfluxdb().setOrg((String) inf.get("org"));
                if (inf.containsKey("bucket")) storage.getInfluxdb().setBucket((String) inf.get("bucket"));
                if (inf.containsKey("batch_size")) storage.getInfluxdb().setBatchSize(((Number) inf.get("batch_size")).intValue());
            }
            if (st.containsKey("postgres")) {
                Map<String, Object> pg = (Map<String, Object>) st.get("postgres");
                if (pg.containsKey("host")) storage.getPostgres().setHost((String) pg.get("host"));
                if (pg.containsKey("port")) storage.getPostgres().setPort(((Number) pg.get("port")).intValue());
                if (pg.containsKey("database")) storage.getPostgres().setDatabase((String) pg.get("database"));
                if (pg.containsKey("username")) storage.getPostgres().setUsername((String) pg.get("username"));
                if (pg.containsKey("password")) storage.getPostgres().setPassword((String) pg.get("password"));
                if (pg.containsKey("max_pool_size")) storage.getPostgres().setMaxPoolSize(((Number) pg.get("max_pool_size")).intValue());
            }
        }
    }

    private void applyEnvOverrides() {
        String envPort = System.getenv("GATEWAY_PORT");
        if (envPort != null) server.setPort(Integer.parseInt(envPort));

        String envInfluxUrl = System.getenv("INFLUXDB_URL");
        if (envInfluxUrl != null) storage.getInfluxdb().setUrl(envInfluxUrl);

        String envInfluxToken = System.getenv("INFLUXDB_TOKEN");
        if (envInfluxToken != null) storage.getInfluxdb().setToken(envInfluxToken);

        String envPgHost = System.getenv("POSTGRES_HOST");
        if (envPgHost != null) storage.getPostgres().setHost(envPgHost);

        String envPgPass = System.getenv("POSTGRES_PASSWORD");
        if (envPgPass != null) storage.getPostgres().setPassword(envPgPass);
    }

    public ServerConfig getServer() { return server; }
    public StorageConfig getStorage() { return storage; }
    public PipelineConfig getPipeline() { return pipeline; }
    public LoggingConfig getLogging() { return logging; }

    public static class ServerConfig {
        private String host = "0.0.0.0";
        private int port = 9090;
        private int bossThreads = 2;
        private int workerThreads = 8;
        private int maxFrameSizeBytes = 10485760;

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public int getBossThreads() { return bossThreads; }
        public void setBossThreads(int bossThreads) { this.bossThreads = bossThreads; }
        public int getWorkerThreads() { return workerThreads; }
        public void setWorkerThreads(int workerThreads) { this.workerThreads = workerThreads; }
        public int getMaxFrameSizeBytes() { return maxFrameSizeBytes; }
        public void setMaxFrameSizeBytes(int maxFrameSizeBytes) { this.maxFrameSizeBytes = maxFrameSizeBytes; }
    }

    public static class StorageConfig {
        private InfluxConfig influxdb = new InfluxConfig();
        private PostgresConfig postgres = new PostgresConfig();

        public InfluxConfig getInfluxdb() { return influxdb; }
        public PostgresConfig getPostgres() { return postgres; }
    }

    public static class InfluxConfig {
        private String url = "http://localhost:8086";
        private String token = "my-super-secret-token";
        private String org = "log-aggregator";
        private String bucket = "metrics";
        private int batchSize = 1000;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getOrg() { return org; }
        public void setOrg(String org) { this.org = org; }
        public String getBucket() { return bucket; }
        public void setBucket(String bucket) { this.bucket = bucket; }
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    }

    public static class PostgresConfig {
        private String host = "localhost";
        private int port = 5432;
        private String database = "log_aggregator";
        private String username = "aggregator";
        private String password = "aggregator_pass";
        private int maxPoolSize = 10;

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getDatabase() { return database; }
        public void setDatabase(String database) { this.database = database; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
    }

    public static class PipelineConfig {
        private int maxAgeHours = 24;
        private int maxFutureSeconds = 60;
        private int maxLogMessageLength = 4096;

        public int getMaxAgeHours() { return maxAgeHours; }
        public void setMaxAgeHours(int maxAgeHours) { this.maxAgeHours = maxAgeHours; }
        public int getMaxFutureSeconds() { return maxFutureSeconds; }
        public void setMaxFutureSeconds(int maxFutureSeconds) { this.maxFutureSeconds = maxFutureSeconds; }
        public int getMaxLogMessageLength() { return maxLogMessageLength; }
        public void setMaxLogMessageLength(int maxLogMessageLength) { this.maxLogMessageLength = maxLogMessageLength; }
    }

    public static class LoggingConfig {
        private String level = "INFO";
        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
    }
}

