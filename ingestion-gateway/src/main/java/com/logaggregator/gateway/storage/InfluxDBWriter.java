package com.logaggregator.gateway.storage;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.logaggregator.gateway.config.GatewayConfig;
import com.logaggregator.gateway.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class InfluxDBWriter implements StorageWriter {

    private static final Logger logger = LoggerFactory.getLogger(InfluxDBWriter.class);

    private final String bucket;
    private final String org;
    private final InfluxDBClient client;
    private final WriteApiBlocking writeApi;

    public InfluxDBWriter(GatewayConfig.InfluxConfig config) {
        this.bucket = config.getBucket();
        this.org = config.getOrg();

        InfluxDBClient c = null;
        WriteApiBlocking w = null;
        try {
            c = InfluxDBClientFactory.create(
                    config.getUrl(),
                    config.getToken().toCharArray(),
                    config.getOrg(),
                    config.getBucket()
            );
            w = c.getWriteApiBlocking();
            logger.info("Connected to InfluxDB at {} [Org: {}, Bucket: {}]", config.getUrl(), org, bucket);
        } catch (Exception e) {
            logger.warn("InfluxDB connection deferred (host may not be reachable yet): {}", e.getMessage());
        }

        this.client = c;
        this.writeApi = w;
    }

    @Override
    public String getName() {
        return "InfluxDBWriter";
    }

    @Override
    public void write(MetricPayload payload) {
        if (writeApi == null) {
            return;
        }

        try {
            List<Point> points = convertToPoints(payload);
            if (!points.isEmpty()) {
                writeApi.writePoints(bucket, org, points);
            }
        } catch (Exception e) {
            logger.error("Failed writing time-series points to InfluxDB: {}", e.getMessage());
        }
    }

    public List<Point> convertToPoints(MetricPayload payload) {
        List<Point> points = new ArrayList<>();
        Instant timestamp = Instant.ofEpochMilli(payload.getTimestamp());

        String agentId = payload.getAgentInfo() != null ? payload.getAgentInfo().getAgentId() : "unknown";
        String hostname = payload.getAgentInfo() != null ? payload.getAgentInfo().getHostname() : "unknown";

        CpuMetrics cpu = payload.getCpuMetrics();
        if (cpu != null) {
            Point p = Point.measurement("cpu_metrics")
                    .addTag("agent_id", agentId)
                    .addTag("hostname", hostname)
                    .addField("user_percent", cpu.getUserPercent())
                    .addField("system_percent", cpu.getSystemPercent())
                    .addField("idle_percent", cpu.getIdlePercent())
                    .addField("iowait_percent", cpu.getIowaitPercent())
                    .addField("steal_percent", cpu.getStealPercent())
                    .addField("core_count", cpu.getCoreCount())
                    .addField("load_avg_1m", cpu.getLoadAvg1m())
                    .addField("load_avg_5m", cpu.getLoadAvg5m())
                    .addField("load_avg_15m", cpu.getLoadAvg15m())
                    .time(timestamp, WritePrecision.MS);
            points.add(p);

            if (cpu.getPerCoreUsage() != null) {
                for (CpuMetrics.PerCoreUsage core : cpu.getPerCoreUsage()) {
                    Point cp = Point.measurement("cpu_core_metrics")
                            .addTag("agent_id", agentId)
                            .addTag("hostname", hostname)
                            .addTag("core_id", String.valueOf(core.getCoreId()))
                            .addField("usage_percent", core.getUsagePercent())
                            .time(timestamp, WritePrecision.MS);
                    points.add(cp);
                }
            }
        }

        MemoryMetrics mem = payload.getMemoryMetrics();
        if (mem != null) {
            Point p = Point.measurement("memory_metrics")
                    .addTag("agent_id", agentId)
                    .addTag("hostname", hostname)
                    .addField("total_bytes", mem.getTotalBytes())
                    .addField("available_bytes", mem.getAvailableBytes())
                    .addField("used_bytes", mem.getUsedBytes())
                    .addField("used_percent", mem.getUsedPercent())
                    .addField("swap_total_bytes", mem.getSwapTotalBytes())
                    .addField("swap_used_bytes", mem.getSwapUsedBytes())
                    .addField("buffers_bytes", mem.getBuffersBytes())
                    .addField("cached_bytes", mem.getCachedBytes())
                    .time(timestamp, WritePrecision.MS);
            points.add(p);
        }

        DiskMetrics disk = payload.getDiskMetrics();
        if (disk != null && disk.getDevices() != null) {
            for (DiskMetrics.DiskDevice dev : disk.getDevices()) {
                Point p = Point.measurement("disk_metrics")
                        .addTag("agent_id", agentId)
                        .addTag("hostname", hostname)
                        .addTag("device", dev.getDeviceName())
                        .addField("reads_completed", dev.getReadsCompleted())
                        .addField("writes_completed", dev.getWritesCompleted())
                        .addField("read_bytes", dev.getReadBytes())
                        .addField("write_bytes", dev.getWriteBytes())
                        .addField("read_rate_bps", dev.getReadRateBps())
                        .addField("write_rate_bps", dev.getWriteRateBps())
                        .addField("utilization_percent", dev.getUtilizationPercent())
                        .addField("total_bytes", dev.getTotalBytes())
                        .addField("used_bytes", dev.getUsedBytes())
                        .addField("used_percent", dev.getUsedPercent())
                        .time(timestamp, WritePrecision.MS);
                points.add(p);
            }
        }

        NetworkMetrics net = payload.getNetworkMetrics();
        if (net != null && net.getInterfaces() != null) {
            for (NetworkMetrics.NetworkInterface iface : net.getInterfaces()) {
                Point p = Point.measurement("network_metrics")
                        .addTag("agent_id", agentId)
                        .addTag("hostname", hostname)
                        .addTag("interface", iface.getName())
                        .addField("bytes_sent", iface.getBytesSent())
                        .addField("bytes_recv", iface.getBytesRecv())
                        .addField("packets_sent", iface.getPacketsSent())
                        .addField("packets_recv", iface.getPacketsRecv())
                        .addField("errors_in", iface.getErrorsIn())
                        .addField("errors_out", iface.getErrorsOut())
                        .addField("send_rate_bps", iface.getSendRateBps())
                        .addField("recv_rate_bps", iface.getRecvRateBps())
                        .time(timestamp, WritePrecision.MS);
                points.add(p);
            }
        }

        return points;
    }

    @Override
    public void flush() {}

    @Override
    public void close() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception ignored) {}
        }
    }
}

