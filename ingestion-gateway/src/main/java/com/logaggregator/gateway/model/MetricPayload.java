package com.logaggregator.gateway.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MetricPayload {

    private AgentInfo agentInfo;

    private long timestamp;

    private long sequenceNumber;

    private CpuMetrics cpuMetrics;

    private MemoryMetrics memoryMetrics;

    private DiskMetrics diskMetrics;

    private NetworkMetrics networkMetrics;

    private List<LogEntry> logEntries;

    private Map<String, String> tags;

    private int priority;

    private long processingTimestamp;

    public MetricPayload() {
        this.logEntries = new ArrayList<>();
        this.tags = new HashMap<>();
    }

    public MetricPayload(AgentInfo agentInfo, long timestamp, long sequenceNumber) {
        this.agentInfo = agentInfo;
        this.timestamp = timestamp;
        this.sequenceNumber = sequenceNumber;
        this.logEntries = new ArrayList<>();
        this.tags = new HashMap<>();
    }

    public AgentInfo getAgentInfo() { return agentInfo; }
    public void setAgentInfo(AgentInfo agentInfo) { this.agentInfo = agentInfo; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public long getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(long sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    public CpuMetrics getCpuMetrics() { return cpuMetrics; }
    public void setCpuMetrics(CpuMetrics cpuMetrics) { this.cpuMetrics = cpuMetrics; }

    public MemoryMetrics getMemoryMetrics() { return memoryMetrics; }
    public void setMemoryMetrics(MemoryMetrics memoryMetrics) { this.memoryMetrics = memoryMetrics; }

    public DiskMetrics getDiskMetrics() { return diskMetrics; }
    public void setDiskMetrics(DiskMetrics diskMetrics) { this.diskMetrics = diskMetrics; }

    public NetworkMetrics getNetworkMetrics() { return networkMetrics; }
    public void setNetworkMetrics(NetworkMetrics networkMetrics) { this.networkMetrics = networkMetrics; }

    public List<LogEntry> getLogEntries() { return logEntries; }
    public void setLogEntries(List<LogEntry> logEntries) {
        this.logEntries = logEntries != null ? logEntries : new ArrayList<>();
    }

    public Map<String, String> getTags() { return tags; }
    public void setTags(Map<String, String> tags) {
        this.tags = tags != null ? tags : new HashMap<>();
    }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public long getProcessingTimestamp() { return processingTimestamp; }
    public void setProcessingTimestamp(long processingTimestamp) { this.processingTimestamp = processingTimestamp; }

    public void addTag(String key, String value) {
        this.tags.put(key, value);
    }

    public void addLogEntry(LogEntry entry) {
        this.logEntries.add(entry);
    }

    public boolean hasMetrics() {
        return cpuMetrics != null || memoryMetrics != null
                || diskMetrics != null || networkMetrics != null;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private AgentInfo agentInfo;
        private long timestamp;
        private long sequenceNumber;
        private CpuMetrics cpuMetrics;
        private MemoryMetrics memoryMetrics;
        private DiskMetrics diskMetrics;
        private NetworkMetrics networkMetrics;
        private List<LogEntry> logEntries = new ArrayList<>();
        private Map<String, String> tags = new HashMap<>();
        private int priority;

        public Builder agentInfo(AgentInfo v) { this.agentInfo = v; return this; }
        public Builder timestamp(long v) { this.timestamp = v; return this; }
        public Builder sequenceNumber(long v) { this.sequenceNumber = v; return this; }
        public Builder cpuMetrics(CpuMetrics v) { this.cpuMetrics = v; return this; }
        public Builder memoryMetrics(MemoryMetrics v) { this.memoryMetrics = v; return this; }
        public Builder diskMetrics(DiskMetrics v) { this.diskMetrics = v; return this; }
        public Builder networkMetrics(NetworkMetrics v) { this.networkMetrics = v; return this; }
        public Builder logEntries(List<LogEntry> v) { this.logEntries = v; return this; }
        public Builder tags(Map<String, String> v) { this.tags = v; return this; }
        public Builder priority(int v) { this.priority = v; return this; }

        public MetricPayload build() {
            MetricPayload p = new MetricPayload(agentInfo, timestamp, sequenceNumber);
            p.setCpuMetrics(cpuMetrics);
            p.setMemoryMetrics(memoryMetrics);
            p.setDiskMetrics(diskMetrics);
            p.setNetworkMetrics(networkMetrics);
            p.setLogEntries(logEntries);
            p.setTags(tags);
            p.setPriority(priority);
            return p;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetricPayload that = (MetricPayload) o;
        return timestamp == that.timestamp
                && sequenceNumber == that.sequenceNumber
                && priority == that.priority
                && Objects.equals(agentInfo, that.agentInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agentInfo, timestamp, sequenceNumber, priority);
    }

    @Override
    public String toString() {
        return "MetricPayload{agent=" + (agentInfo != null ? agentInfo.getAgentId() : "null") +
                ", timestamp=" + timestamp + ", seq=" + sequenceNumber +
                ", priority=" + priority +
                ", hasCpu=" + (cpuMetrics != null) +
                ", hasMem=" + (memoryMetrics != null) +
                ", hasDisk=" + (diskMetrics != null) +
                ", hasNet=" + (networkMetrics != null) +
                ", logs=" + logEntries.size() +
                ", tags=" + tags.size() + '}';
    }
}
