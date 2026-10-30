package com.logaggregator.gateway.routing;

import com.logaggregator.gateway.model.MetricPayload;
import com.logaggregator.gateway.storage.InfluxDBWriter;
import com.logaggregator.gateway.storage.PostgresWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricTypeRouter implements RoutingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(MetricTypeRouter.class);

    private final InfluxDBWriter influxWriter;
    private final PostgresWriter postgresWriter;

    public MetricTypeRouter(InfluxDBWriter influxWriter, PostgresWriter postgresWriter) {
        this.influxWriter = influxWriter;
        this.postgresWriter = postgresWriter;
    }

    @Override
    public String getStrategyName() {
        return "MetricTypeRoutingStrategy";
    }

    @Override
    public void route(MetricPayload payload) {

        if (influxWriter != null && (payload.getCpuMetrics() != null || payload.getMemoryMetrics() != null ||
                payload.getDiskMetrics() != null || payload.getNetworkMetrics() != null)) {
            influxWriter.write(payload);
        }

        if (postgresWriter != null) {
            postgresWriter.write(payload);
        }
    }
}

