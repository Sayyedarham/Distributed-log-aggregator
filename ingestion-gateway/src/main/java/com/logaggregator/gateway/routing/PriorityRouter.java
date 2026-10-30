package com.logaggregator.gateway.routing;

import com.logaggregator.gateway.events.EventBus;
import com.logaggregator.gateway.events.EventType;
import com.logaggregator.gateway.events.SystemEvent;
import com.logaggregator.gateway.model.MetricPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class PriorityRouter implements RoutingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(PriorityRouter.class);

    private final EventBus eventBus;

    public PriorityRouter(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public String getStrategyName() {
        return "PriorityRoutingStrategy";
    }

    @Override
    public void route(MetricPayload payload) {
        if (payload.getPriority() >= 2) {
            logger.warn("Urgent priority ({}) telemetry received from agent {}",
                    payload.getPriority(),
                    payload.getAgentInfo() != null ? payload.getAgentInfo().getAgentId() : "unknown");

            if (eventBus != null) {
                Map<String, Object> data = new HashMap<>();
                data.put("priority", payload.getPriority());
                data.put("seq", payload.getSequenceNumber());
                eventBus.publish(new SystemEvent(
                        EventType.THROUGHPUT_ALERT,
                        payload.getAgentInfo() != null ? payload.getAgentInfo().getAgentId() : null,
                        data
                ));
            }
        }
    }
}

