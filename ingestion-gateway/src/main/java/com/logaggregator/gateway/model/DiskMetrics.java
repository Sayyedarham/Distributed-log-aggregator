package com.logaggregator.gateway.model;

import java.util.Objects;

public class DiskMetrics {

    private long totalBytes;

    private long usedBytes;

    private long freeBytes;

    private double usagePercent;

    private long readBytesPerSec;

    private long writeBytesPerSec;

    private long readIops;

    private long writeIops;

    public DiskMetrics() {
    }

    public DiskMetrics(long totalBytes, long usedBytes, long freeBytes,
                       double usagePercent, long readBytesPerSec,
                       long writeBytesPerSec, long readIops, long writeIops) {
        this.totalBytes = totalBytes;
        this.usedBytes = usedBytes;
        this.freeBytes = freeBytes;
        this.usagePercent = usagePercent;
        this.readBytesPerSec = readBytesPerSec;
        this.writeBytesPerSec = writeBytesPerSec;
        this.readIops = readIops;
        this.writeIops = writeIops;
    }

    public long getTotalBytes() { return totalBytes; }
    public void setTotalBytes(long totalBytes) { this.totalBytes = totalBytes; }

    public long getUsedBytes() { return usedBytes; }
    public void setUsedBytes(long usedBytes) { this.usedBytes = usedBytes; }

    public long getFreeBytes() { return freeBytes; }
    public void setFreeBytes(long freeBytes) { this.freeBytes = freeBytes; }

    public double getUsagePercent() { return usagePercent; }
    public void setUsagePercent(double usagePercent) { this.usagePercent = usagePercent; }

    public long getReadBytesPerSec() { return readBytesPerSec; }
    public void setReadBytesPerSec(long readBytesPerSec) { this.readBytesPerSec = readBytesPerSec; }

    public long getWriteBytesPerSec() { return writeBytesPerSec; }
    public void setWriteBytesPerSec(long writeBytesPerSec) { this.writeBytesPerSec = writeBytesPerSec; }

    public long getReadIops() { return readIops; }
    public void setReadIops(long readIops) { this.readIops = readIops; }

    public long getWriteIops() { return writeIops; }
    public void setWriteIops(long writeIops) { this.writeIops = writeIops; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private long totalBytes;
        private long usedBytes;
        private long freeBytes;
        private double usagePercent;
        private long readBytesPerSec;
        private long writeBytesPerSec;
        private long readIops;
        private long writeIops;

        public Builder totalBytes(long v) { this.totalBytes = v; return this; }
        public Builder usedBytes(long v) { this.usedBytes = v; return this; }
        public Builder freeBytes(long v) { this.freeBytes = v; return this; }
        public Builder usagePercent(double v) { this.usagePercent = v; return this; }
        public Builder readBytesPerSec(long v) { this.readBytesPerSec = v; return this; }
        public Builder writeBytesPerSec(long v) { this.writeBytesPerSec = v; return this; }
        public Builder readIops(long v) { this.readIops = v; return this; }
        public Builder writeIops(long v) { this.writeIops = v; return this; }

        public DiskMetrics build() {
            return new DiskMetrics(totalBytes, usedBytes, freeBytes, usagePercent,
                    readBytesPerSec, writeBytesPerSec, readIops, writeIops);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiskMetrics that = (DiskMetrics) o;
        return totalBytes == that.totalBytes && usedBytes == that.usedBytes
                && freeBytes == that.freeBytes
                && Double.compare(that.usagePercent, usagePercent) == 0
                && readBytesPerSec == that.readBytesPerSec
                && writeBytesPerSec == that.writeBytesPerSec
                && readIops == that.readIops && writeIops == that.writeIops;
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalBytes, usedBytes, freeBytes, usagePercent,
                readBytesPerSec, writeBytesPerSec, readIops, writeIops);
    }

    @Override
    public String toString() {
        return "DiskMetrics{total=" + totalBytes + ", used=" + usedBytes +
                ", free=" + freeBytes + ", usage=" + usagePercent +
                "%, readBps=" + readBytesPerSec + ", writeBps=" + writeBytesPerSec +
                ", readIOPS=" + readIops + ", writeIOPS=" + writeIops + '}';
    }
}
