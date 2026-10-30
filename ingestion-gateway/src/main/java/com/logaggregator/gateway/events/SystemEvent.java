package com.logaggregator.gateway.events;

import java.util.Collections;
import java.util.Map;

public class SystemEvent {

    private final EventType type;
    private final long timestamp;
    private final String agentId;
    private final Map<String, Object> data;

    public SystemEvent(EventType type, String agentId, Map<String, Object> data) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.agentId = agentId;
        this.data = data != null ? data : Collections.emptyMap();
    }

    public EventType getType() { return type; }
    public long getTimestamp() { return timestamp; }
    public String getAgentId() { return agentId; }
    public Map<String, Object> getData() { return data; }

    @Override
    public String toString() {
        return "SystemEvent{" +
                "type=" + type +
                ", timestamp=" + timestamp +
                ", agentId='" + agentId + '\'' +
                ", data=" + data +
                '}';
    }
}

