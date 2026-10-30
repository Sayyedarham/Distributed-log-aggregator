package com.logaggregator.gateway.state;

import com.logaggregator.gateway.model.AgentInfo;

public class AgentState {

    private final AgentInfo agentInfo;
    private final long connectionTime;
    private volatile long lastHeartbeatTime;
    private volatile long lastSequenceNumber;
    private volatile long totalPayloadsReceived;
    private volatile boolean active;

    public AgentState(AgentInfo agentInfo) {
        this.agentInfo = agentInfo;
        this.connectionTime = System.currentTimeMillis();
        this.lastHeartbeatTime = this.connectionTime;
        this.lastSequenceNumber = 0;
        this.totalPayloadsReceived = 0;
        this.active = true;
    }

    public void recordHeartbeat(long sequenceNumber) {
        this.lastHeartbeatTime = System.currentTimeMillis();
        this.lastSequenceNumber = sequenceNumber;
        this.totalPayloadsReceived++;
        this.active = true;
    }

    public AgentInfo getAgentInfo() { return agentInfo; }
    public long getConnectionTime() { return connectionTime; }
    public long getLastHeartbeatTime() { return lastHeartbeatTime; }
    public long getLastSequenceNumber() { return lastSequenceNumber; }
    public long getTotalPayloadsReceived() { return totalPayloadsReceived; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

