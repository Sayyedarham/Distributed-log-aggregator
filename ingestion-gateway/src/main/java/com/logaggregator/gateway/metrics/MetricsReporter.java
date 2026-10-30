package com.logaggregator.gateway.metrics;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.logaggregator.gateway.storage.InfluxDBWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MetricsReporter {

    private static final Logger logger = LoggerFactory.getLogger(MetricsReporter.class);

    private final GatewayMetrics metrics;
    private final InfluxDBWriter influxWriter;
    private final ScheduledExecutorService scheduler;
    private long lastMessages = 0;
    private long lastBytes = 0;

    public MetricsReporter(GatewayMetrics metrics, InfluxDBWriter influxWriter) {
        this.metrics = metrics;
        this.influxWriter = influxWriter;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "gateway-metrics-reporter");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::report, 10, 10, TimeUnit.SECONDS);
    }

    private void report() {
        long currentMsgs = metrics.getMessagesReceived();
        long currentBytes = metrics.getBytesReceived();

        double msgRate = (currentMsgs - lastMessages) / 10.0;
        double byteRate = (currentBytes - lastBytes) / 10.0;

        lastMessages = currentMsgs;
        lastBytes = currentBytes;

        logger.info("[Gateway Telemetry] Active Sockets: {} | Msg Rate: {}/s | Throughput: {} KB/s | Avg Latency: {} ms",
                metrics.getActiveConnections(),
                String.format("%.1f", msgRate),
                String.format("%.1f", byteRate / 1024.0),
                String.format("%.2f", metrics.getAvgPipelineLatencyMs()));
    }

    public void stop() {
        scheduler.shutdown();
    }
}

