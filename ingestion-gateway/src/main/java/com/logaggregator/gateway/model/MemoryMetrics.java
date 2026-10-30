package com.logaggregator.gateway.model;

import java.util.Objects;

public class MemoryMetrics {

    private long totalBytes;

    private long availableBytes;

    private long usedBytes;

    private double usagePercent;

    private long cachedBytes;

    private long swapTotalBytes;

    private long swapUsedBytes;

    private double swapPercent;

    public MemoryMetrics() {
    }

    public MemoryMetrics(long totalBytes, long availableBytes, long usedBytes,
                         double usagePercent, long cachedBytes,
                         long swapTotalBytes, long swapUsedBytes, double swapPercent) {
        this.totalBytes = totalBytes;
        this.availableBytes = availableBytes;
        this.usedBytes = usedBytes;
        this.usagePercent = usagePercent;
        this.cachedBytes = cachedBytes;
        this.swapTotalBytes = swapTotalBytes;
        this.swapUsedBytes = swapUsedBytes;
        this.swapPercent = swapPercent;
    }

    public long getTotalBytes() { return totalBytes; }
    public void setTotalBytes(long totalBytes) { this.totalBytes = totalBytes; }

    public long getAvailableBytes() { return availableBytes; }
    public void setAvailableBytes(long availableBytes) { this.availableBytes = availableBytes; }

    public long getUsedBytes() { return usedBytes; }
    public void setUsedBytes(long usedBytes) { this.usedBytes = usedBytes; }

    public double getUsagePercent() { return usagePercent; }
    public void setUsagePercent(double usagePercent) { this.usagePercent = usagePercent; }

    public long getCachedBytes() { return cachedBytes; }
    public void setCachedBytes(long cachedBytes) { this.cachedBytes = cachedBytes; }

    public long getSwapTotalBytes() { return swapTotalBytes; }
    public void setSwapTotalBytes(long swapTotalBytes) { this.swapTotalBytes = swapTotalBytes; }

    public long getSwapUsedBytes() { return swapUsedBytes; }
    public void setSwapUsedBytes(long swapUsedBytes) { this.swapUsedBytes = swapUsedBytes; }

    public double getSwapPercent() { return swapPercent; }
    public void setSwapPercent(double swapPercent) { this.swapPercent = swapPercent; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private long totalBytes;
        private long availableBytes;
        private long usedBytes;
        private double usagePercent;
        private long cachedBytes;
        private long swapTotalBytes;
        private long swapUsedBytes;
        private double swapPercent;

        public Builder totalBytes(long v) { this.totalBytes = v; return this; }
        public Builder availableBytes(long v) { this.availableBytes = v; return this; }
        public Builder usedBytes(long v) { this.usedBytes = v; return this; }
        public Builder usagePercent(double v) { this.usagePercent = v; return this; }
        public Builder cachedBytes(long v) { this.cachedBytes = v; return this; }
        public Builder swapTotalBytes(long v) { this.swapTotalBytes = v; return this; }
        public Builder swapUsedBytes(long v) { this.swapUsedBytes = v; return this; }
        public Builder swapPercent(double v) { this.swapPercent = v; return this; }

        public MemoryMetrics build() {
            return new MemoryMetrics(totalBytes, availableBytes, usedBytes,
                    usagePercent, cachedBytes, swapTotalBytes, swapUsedBytes, swapPercent);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemoryMetrics that = (MemoryMetrics) o;
        return totalBytes == that.totalBytes
                && availableBytes == that.availableBytes
                && usedBytes == that.usedBytes
                && Double.compare(that.usagePercent, usagePercent) == 0
                && cachedBytes == that.cachedBytes
                && swapTotalBytes == that.swapTotalBytes
                && swapUsedBytes == that.swapUsedBytes
                && Double.compare(that.swapPercent, swapPercent) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalBytes, availableBytes, usedBytes, usagePercent,
                cachedBytes, swapTotalBytes, swapUsedBytes, swapPercent);
    }

    @Override
    public String toString() {
        return "MemoryMetrics{total=" + totalBytes + ", available=" + availableBytes +
                ", used=" + usedBytes + ", usage=" + usagePercent +
                "%, cached=" + cachedBytes + ", swapTotal=" + swapTotalBytes +
                ", swapUsed=" + swapUsedBytes + ", swap=" + swapPercent + "%}";
    }
}
