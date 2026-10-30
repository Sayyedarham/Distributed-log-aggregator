package com.logaggregator.gateway.pipeline;

import com.logaggregator.gateway.model.CpuMetrics;
import com.logaggregator.gateway.model.MetricPayload;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AggregationStage extends PipelineStage {

    private final Map<String, RunningAvg> agentCpuAverages = new ConcurrentHashMap<>();

    @Override
    public String getName() {
        return "AggregationStage";
    }

    @Override
    public MetricPayload process(MetricPayload payload) throws PipelineException {
        if (payload.getCpuMetrics() != null && payload.getAgentInfo() != null) {
            String agentId = payload.getAgentInfo().getAgentId();
            double userPct = payload.getCpuMetrics().getUserPercent();

            RunningAvg avg = agentCpuAverages.computeIfAbsent(agentId, k -> new RunningAvg());
            avg.addSample(userPct);

            payload.getTags().put("aggregated_cpu_mean", String.format("%.2f", avg.getAverage()));
        }
        return payload;
    }

    private static class RunningAvg {
        private double sum = 0.0;
        private int count = 0;

        public synchronized void addSample(double val) {
            sum += val;
            count++;

            if (count > 60) {
                sum = (sum / count) * 30;
                count = 30;
            }
        }

        public synchronized double getAverage() {
            return count > 0 ? (sum / count) : 0.0;
        }
    }
}

