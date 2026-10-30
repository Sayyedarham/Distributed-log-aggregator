package com.logaggregator.gateway.metrics;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

public class GatewayMetrics {

    private final AtomicLong messagesReceived = new AtomicLong(0);
    private final AtomicLong bytesReceived = new AtomicLong(0);
    private final AtomicLong batchesProcessed = new AtomicLong(0);
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicLong pipelineErrors = new AtomicLong(0);
    private final DoubleAdder totalPipelineLatencyMs = new DoubleAdder();
    private final AtomicLong latencyMeasurementCount = new AtomicLong(0);

    public void recordMessage(int byteSize) {
        messagesReceived.incrementAndGet();
        bytesReceived.addAndGet(byteSize);
    }

    public void recordBatch() {
        batchesProcessed.incrementAndGet();
    }

    public void recordLatency(double latencyMs) {
        totalPipelineLatencyMs.add(latencyMs);
        latencyMeasurementCount.incrementAndGet();
    }

    public void recordError() {
        pipelineErrors.incrementAndGet();
    }

    public void incrementConnections() {
        activeConnections.incrementAndGet();
    }

    public void decrementConnections() {
        activeConnections.decrementAndGet();
    }

    public long getMessagesReceived() { return messagesReceived.get(); }
    public long getBytesReceived() { return bytesReceived.get(); }
    public long getBatchesProcessed() { return batchesProcessed.get(); }
    public int getActiveConnections() { return activeConnections.get(); }
    public long getPipelineErrors() { return pipelineErrors.get(); }

    public double getAvgPipelineLatencyMs() {
        long count = latencyMeasurementCount.get();
        return count > 0 ? (totalPipelineLatencyMs.sum() / count) : 0.0;
    }
}

