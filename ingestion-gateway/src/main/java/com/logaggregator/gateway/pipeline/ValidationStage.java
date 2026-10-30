package com.logaggregator.gateway.pipeline;

import com.logaggregator.gateway.config.GatewayConfig;
import com.logaggregator.gateway.model.CpuMetrics;
import com.logaggregator.gateway.model.MemoryMetrics;
import com.logaggregator.gateway.model.MetricPayload;

public class ValidationStage extends PipelineStage {

    private final long maxPastMs;
    private final long maxFutureMs;

    public ValidationStage(GatewayConfig.PipelineConfig config) {
        this.maxPastMs = (long) config.getMaxAgeHours() * 3600 * 1000;
        this.maxFutureMs = (long) config.getMaxFutureSeconds() * 1000;
    }

    @Override
    public String getName() {
        return "ValidationStage";
    }

    @Override
    public MetricPayload process(MetricPayload payload) throws PipelineException {
        if (payload == null) {
            throw new PipelineException(getName(), "Null payload rejected");
        }

        if (payload.getAgentInfo() == null || payload.getAgentInfo().getAgentId() == null ||
                payload.getAgentInfo().getAgentId().trim().isEmpty()) {
            throw new PipelineException(getName(), "Missing or empty Agent ID");
        }

        long now = System.currentTimeMillis();
        long ts = payload.getTimestamp();

        if (ts > now + maxFutureMs) {
            throw new PipelineException(getName(), "Payload timestamp is too far in future: " + ts + " (now=" + now + ")");
        }

        if (now - ts > maxPastMs) {
            throw new PipelineException(getName(), "Payload timestamp is expired: " + ts + " (now=" + now + ")");
        }

        CpuMetrics cpu = payload.getCpuMetrics();
        if (cpu != null) {
            if (cpu.getUserPercent() < 0.0 || cpu.getUserPercent() > 100.0) {
                cpu.setUserPercent(Math.max(0.0, Math.min(100.0, cpu.getUserPercent())));
            }
            if (cpu.getSystemPercent() < 0.0 || cpu.getSystemPercent() > 100.0) {
                cpu.setSystemPercent(Math.max(0.0, Math.min(100.0, cpu.getSystemPercent())));
            }
        }

        MemoryMetrics mem = payload.getMemoryMetrics();
        if (mem != null) {
            if (mem.getUsedPercent() < 0.0 || mem.getUsedPercent() > 100.0) {
                mem.setUsedPercent(Math.max(0.0, Math.min(100.0, mem.getUsedPercent())));
            }
        }

        return payload;
    }
}

