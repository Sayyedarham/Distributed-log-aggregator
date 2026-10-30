package com.logaggregator.gateway.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AgentRegistration {

    private AgentInfo agentInfo;

    private int collectionIntervalSec;

    private Map<String, Boolean> capabilities;

    private long timestamp;

    public AgentRegistration() {
        this.capabilities = new HashMap<>();
    }

    public AgentRegistration(AgentInfo agentInfo, int collectionIntervalSec,
                             Map<String, Boolean> capabilities, long timestamp) {
        this.agentInfo = agentInfo;
        this.collectionIntervalSec = collectionIntervalSec;
        this.capabilities = capabilities != null ? capabilities : new HashMap<>();
        this.timestamp = timestamp;
    }

    public AgentInfo getAgentInfo() { return agentInfo; }
    public void setAgentInfo(AgentInfo agentInfo) { this.agentInfo = agentInfo; }

    public int getCollectionIntervalSec() { return collectionIntervalSec; }
    public void setCollectionIntervalSec(int collectionIntervalSec) { this.collectionIntervalSec = collectionIntervalSec; }

    public Map<String, Boolean> getCapabilities() { return capabilities; }
    public void setCapabilities(Map<String, Boolean> capabilities) {
        this.capabilities = capabilities != null ? capabilities : new HashMap<>();
    }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private AgentInfo agentInfo;
        private int collectionIntervalSec;
        private Map<String, Boolean> capabilities = new HashMap<>();
        private long timestamp;

        public Builder agentInfo(AgentInfo v) { this.agentInfo = v; return this; }
        public Builder collectionIntervalSec(int v) { this.collectionIntervalSec = v; return this; }
        public Builder capabilities(Map<String, Boolean> v) { this.capabilities = v; return this; }
        public Builder timestamp(long v) { this.timestamp = v; return this; }

        public AgentRegistration build() {
            return new AgentRegistration(agentInfo, collectionIntervalSec, capabilities, timestamp);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentRegistration that = (AgentRegistration) o;
        return collectionIntervalSec == that.collectionIntervalSec
                && timestamp == that.timestamp
                && Objects.equals(agentInfo, that.agentInfo)
                && Objects.equals(capabilities, that.capabilities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agentInfo, collectionIntervalSec, capabilities, timestamp);
    }

    @Override
    public String toString() {
        return "AgentRegistration{agentInfo=" + agentInfo +
                ", intervalSec=" + collectionIntervalSec +
                ", capabilities=" + capabilities +
                ", timestamp=" + timestamp + '}';
    }
}
