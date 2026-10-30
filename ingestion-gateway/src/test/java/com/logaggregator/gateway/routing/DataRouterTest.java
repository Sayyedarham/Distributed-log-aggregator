package com.logaggregator.gateway.routing;

import com.logaggregator.gateway.model.AgentInfo;
import com.logaggregator.gateway.model.MetricPayload;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class DataRouterTest {

    @Test
    public void testStrategyExecution() {
        DataRouter router = new DataRouter();
        AtomicBoolean strategyExecuted = new AtomicBoolean(false);

        router.addStrategy(new RoutingStrategy() {
            @Override
            public void route(MetricPayload payload) {
                strategyExecuted.set(true);
            }

            @Override
            public String getStrategyName() {
                return "MockStrategy";
            }
        });

        MetricPayload payload = new MetricPayload();
        payload.setAgentInfo(new AgentInfo("agent-1", "host-a", "127.0.0.1", "linux", "6.1", 100L, "1.0"));

        router.route(payload);
        assertTrue(strategyExecuted.get());
    }
}

