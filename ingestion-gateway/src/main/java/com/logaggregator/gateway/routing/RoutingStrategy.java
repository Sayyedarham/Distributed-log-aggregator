package com.logaggregator.gateway.routing;

import com.logaggregator.gateway.model.MetricPayload;

public interface RoutingStrategy {

    void route(MetricPayload payload);

    String getStrategyName();
}

