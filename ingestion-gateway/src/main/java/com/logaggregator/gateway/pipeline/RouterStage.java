package com.logaggregator.gateway.pipeline;

import com.logaggregator.gateway.model.MetricPayload;
import com.logaggregator.gateway.routing.DataRouter;

public class RouterStage extends PipelineStage {

    private final DataRouter router;

    public RouterStage(DataRouter router) {
        this.router = router;
    }

    @Override
    public String getName() {
        return "RouterStage";
    }

    @Override
    public MetricPayload process(MetricPayload payload) throws PipelineException {
        if (router != null) {
            router.route(payload);
        }
        return payload;
    }
}

