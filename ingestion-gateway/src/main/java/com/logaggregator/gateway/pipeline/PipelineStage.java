package com.logaggregator.gateway.pipeline;

import com.logaggregator.gateway.model.MetricPayload;

public abstract class PipelineStage {

    public abstract String getName();

    public abstract MetricPayload process(MetricPayload payload) throws PipelineException;

    public boolean shouldProcess(MetricPayload payload) {
        return payload != null;
    }
}

