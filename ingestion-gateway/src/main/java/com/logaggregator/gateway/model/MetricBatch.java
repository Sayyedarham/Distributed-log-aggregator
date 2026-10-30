package com.logaggregator.gateway.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MetricBatch {

    private String batchId;

    private String agentId;

    private long batchTimestamp;

    private List<MetricPayload> payloads;

    public MetricBatch() {
        this.payloads = new ArrayList<>();
    }

    public MetricBatch(String batchId, String agentId, long batchTimestamp,
                       List<MetricPayload> payloads) {
        this.batchId = batchId;
        this.agentId = agentId;
        this.batchTimestamp = batchTimestamp;
        this.payloads = payloads != null ? payloads : new ArrayList<>();
    }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public long getBatchTimestamp() { return batchTimestamp; }
    public void setBatchTimestamp(long batchTimestamp) { this.batchTimestamp = batchTimestamp; }

    public List<MetricPayload> getPayloads() { return payloads; }
    public void setPayloads(List<MetricPayload> payloads) {
        this.payloads = payloads != null ? payloads : new ArrayList<>();
    }

    public int size() {
        return payloads.size();
    }

    public void addPayload(MetricPayload payload) {
        this.payloads.add(payload);
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String batchId;
        private String agentId;
        private long batchTimestamp;
        private List<MetricPayload> payloads = new ArrayList<>();

        public Builder batchId(String v) { this.batchId = v; return this; }
        public Builder agentId(String v) { this.agentId = v; return this; }
        public Builder batchTimestamp(long v) { this.batchTimestamp = v; return this; }
        public Builder payloads(List<MetricPayload> v) { this.payloads = v; return this; }
        public Builder addPayload(MetricPayload v) { this.payloads.add(v); return this; }

        public MetricBatch build() {
            return new MetricBatch(batchId, agentId, batchTimestamp, payloads);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetricBatch that = (MetricBatch) o;
        return batchTimestamp == that.batchTimestamp
                && Objects.equals(batchId, that.batchId)
                && Objects.equals(agentId, that.agentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(batchId, agentId, batchTimestamp);
    }

    @Override
    public String toString() {
        return "MetricBatch{batchId='" + batchId + "', agentId='" + agentId +
                "', timestamp=" + batchTimestamp + ", payloads=" + payloads.size() + '}';
    }
}
