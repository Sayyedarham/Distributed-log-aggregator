package com.logaggregator.gateway.state;

import com.logaggregator.gateway.model.AgentInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AgentRegistry {

    private final Map<String, AgentState> registry = new ConcurrentHashMap<>();

    public void register(AgentInfo info) {
        if (info == null || info.getAgentId() == null) return;
        registry.put(info.getAgentId(), new AgentState(info));
    }

    public void recordActivity(String agentId, long sequenceNumber) {
        if (agentId == null) return;
        AgentState state = registry.get(agentId);
        if (state != null) {
            state.recordHeartbeat(sequenceNumber);
        }
    }

    public AgentState getAgent(String agentId) {
        return registry.get(agentId);
    }

    public boolean isRegistered(String agentId) {
        return registry.containsKey(agentId);
    }

    public int getActiveCount() {
        long threshold = System.currentTimeMillis() - 60000;
        int count = 0;
        for (AgentState s : registry.values()) {
            if (s.getLastHeartbeatTime() >= threshold) {
                count++;
            }
        }
        return count;
    }

    public Collection<AgentState> getAllAgents() {
        return Collections.unmodifiableCollection(registry.values());
    }
}

