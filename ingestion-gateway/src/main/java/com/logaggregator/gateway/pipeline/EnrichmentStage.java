package com.logaggregator.gateway.pipeline;

import com.logaggregator.gateway.model.MetricPayload;

import java.util.Map;

public class EnrichmentStage extends PipelineStage {

    private final String datacenter;
    private final String cluster;

    public EnrichmentStage(String datacenter, String cluster) {
        this.datacenter = datacenter != null ? datacenter : "us-east-1a";
        this.cluster = cluster != null ? cluster : "prod-core-01";
    }

    @Override
    public String getName() {
        return "EnrichmentStage";
    }

    @Override
    public MetricPayload process(MetricPayload payload) throws PipelineException {
        payload.setProcessingTimestamp(System.currentTimeMillis());

        Map<String, String> tags = payload.getTags();
        tags.put("datacenter", datacenter);
        tags.put("cluster", cluster);
        tags.put("gateway_ingest_time", String.valueOf(payload.getProcessingTimestamp()));

        return payload;
    }
}

