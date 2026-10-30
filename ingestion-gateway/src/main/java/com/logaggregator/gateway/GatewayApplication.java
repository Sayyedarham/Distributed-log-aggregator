package com.logaggregator.gateway;

import com.logaggregator.gateway.config.GatewayConfig;
import com.logaggregator.gateway.events.EventBus;
import com.logaggregator.gateway.events.EventType;
import com.logaggregator.gateway.metrics.GatewayMetrics;
import com.logaggregator.gateway.metrics.MetricsReporter;
import com.logaggregator.gateway.pipeline.*;
import com.logaggregator.gateway.routing.DataRouter;
import com.logaggregator.gateway.routing.MetricTypeRouter;
import com.logaggregator.gateway.routing.PriorityRouter;
import com.logaggregator.gateway.server.TcpServer;
import com.logaggregator.gateway.state.AgentRegistry;
import com.logaggregator.gateway.state.StateSynchronizer;
import com.logaggregator.gateway.storage.ConnectionPool;
import com.logaggregator.gateway.storage.InfluxDBWriter;
import com.logaggregator.gateway.storage.PostgresWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GatewayApplication {

    private static final Logger logger = LoggerFactory.getLogger(GatewayApplication.class);

    public static void main(String[] args) {
        printBanner();

        String configPath = null;
        for (int i = 0; i < args.length; i++) {
            if ("--config".equals(args[i]) && i + 1 < args.length) {
                configPath = args[i + 1];
            }
        }

        GatewayConfig config = GatewayConfig.load(configPath);
        logger.info("Loaded Gateway Configuration (TCP Port: {})", config.getServer().getPort());

        GatewayMetrics gatewayMetrics = new GatewayMetrics();
        EventBus eventBus = new EventBus();

        AgentRegistry agentRegistry = new AgentRegistry();

        ConnectionPool connectionPool = new ConnectionPool(config.getStorage().getPostgres());
        PostgresWriter postgresWriter = new PostgresWriter(connectionPool);
        InfluxDBWriter influxWriter = new InfluxDBWriter(config.getStorage().getInfluxdb());

        DataRouter dataRouter = new DataRouter();
        dataRouter.addStrategy(new MetricTypeRouter(influxWriter, postgresWriter));
        dataRouter.addStrategy(new PriorityRouter(eventBus));

        Pipeline pipeline = new Pipeline();
        pipeline.addStage(new ValidationStage(config.getPipeline()));
        pipeline.addStage(new SanitizationStage(config.getPipeline()));
        pipeline.addStage(new AggregationStage());
        pipeline.addStage(new EnrichmentStage("us-east-1a", "prod-core-01"));
        pipeline.addStage(new RouterStage(dataRouter));

        StateSynchronizer stateSync = new StateSynchronizer(agentRegistry, connectionPool);
        stateSync.start();

        MetricsReporter metricsReporter = new MetricsReporter(gatewayMetrics, influxWriter);
        metricsReporter.start();

        TcpServer server = new TcpServer(
                config.getServer(),
                pipeline,
                agentRegistry,
                gatewayMetrics,
                eventBus
        );

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Initiating graceful shutdown of Ingestion Gateway...");
            server.stop();
            stateSync.stop();
            metricsReporter.stop();
            eventBus.shutdown();
            influxWriter.close();
            connectionPool.close();
            logger.info("Ingestion Gateway stopped gracefully.");
        }, "gateway-shutdown-hook"));

        try {
            server.start();
        } catch (InterruptedException e) {
            logger.error("Gateway server execution interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private static void printBanner() {
        System.out.println("=================================================================");
        System.out.println("   DISTRIBUTED LOG AGGREGATOR & METRIC MONITOR - INGESTION TIER   ");
        System.out.println("   High-Throughput Distributed Gateway | Java 17 + Netty NIO     ");
        System.out.println("=================================================================");
    }
}

