package com.logaggregator.gateway.routing;

import com.logaggregator.gateway.model.MetricPayload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DataRouter {

    private final List<RoutingStrategy> strategies = new ArrayList<>();

    public DataRouter addStrategy(RoutingStrategy strategy) {
        if (strategy != null) {
            strategies.add(strategy);
        }
        return this;
    }

    public List<RoutingStrategy> getStrategies() {
        return Collections.unmodifiableList(strategies);
    }

    public void route(MetricPayload payload) {
        if (payload == null) return;
        for (RoutingStrategy s : strategies) {
            s.route(payload);
        }
    }
}

