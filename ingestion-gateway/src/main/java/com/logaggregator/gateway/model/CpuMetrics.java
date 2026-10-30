package com.logaggregator.gateway.model;

import java.util.Objects;

public class CpuMetrics {

    private double usagePercent;

    private double userPercent;

    private double systemPercent;

    private double idlePercent;

    private double iowaitPercent;

    private double loadAvg1;

    private double loadAvg5;

    private double loadAvg15;

    private int coreCount;

    public CpuMetrics() {
    }

    public CpuMetrics(double usagePercent, double userPercent, double systemPercent,
                      double idlePercent, double iowaitPercent,
                      double loadAvg1, double loadAvg5, double loadAvg15,
                      int coreCount) {
        this.usagePercent = usagePercent;
        this.userPercent = userPercent;
        this.systemPercent = systemPercent;
        this.idlePercent = idlePercent;
        this.iowaitPercent = iowaitPercent;
        this.loadAvg1 = loadAvg1;
        this.loadAvg5 = loadAvg5;
        this.loadAvg15 = loadAvg15;
        this.coreCount = coreCount;
    }

    public double getUsagePercent() { return usagePercent; }
    public void setUsagePercent(double usagePercent) { this.usagePercent = usagePercent; }

    public double getUserPercent() { return userPercent; }
    public void setUserPercent(double userPercent) { this.userPercent = userPercent; }

    public double getSystemPercent() { return systemPercent; }
    public void setSystemPercent(double systemPercent) { this.systemPercent = systemPercent; }

    public double getIdlePercent() { return idlePercent; }
    public void setIdlePercent(double idlePercent) { this.idlePercent = idlePercent; }

    public double getIowaitPercent() { return iowaitPercent; }
    public void setIowaitPercent(double iowaitPercent) { this.iowaitPercent = iowaitPercent; }

    public double getLoadAvg1() { return loadAvg1; }
    public void setLoadAvg1(double loadAvg1) { this.loadAvg1 = loadAvg1; }

    public double getLoadAvg5() { return loadAvg5; }
    public void setLoadAvg5(double loadAvg5) { this.loadAvg5 = loadAvg5; }

    public double getLoadAvg15() { return loadAvg15; }
    public void setLoadAvg15(double loadAvg15) { this.loadAvg15 = loadAvg15; }

    public int getCoreCount() { return coreCount; }
    public void setCoreCount(int coreCount) { this.coreCount = coreCount; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private double usagePercent;
        private double userPercent;
        private double systemPercent;
        private double idlePercent;
        private double iowaitPercent;
        private double loadAvg1;
        private double loadAvg5;
        private double loadAvg15;
        private int coreCount;

        public Builder usagePercent(double v) { this.usagePercent = v; return this; }
        public Builder userPercent(double v) { this.userPercent = v; return this; }
        public Builder systemPercent(double v) { this.systemPercent = v; return this; }
        public Builder idlePercent(double v) { this.idlePercent = v; return this; }
        public Builder iowaitPercent(double v) { this.iowaitPercent = v; return this; }
        public Builder loadAvg1(double v) { this.loadAvg1 = v; return this; }
        public Builder loadAvg5(double v) { this.loadAvg5 = v; return this; }
        public Builder loadAvg15(double v) { this.loadAvg15 = v; return this; }
        public Builder coreCount(int v) { this.coreCount = v; return this; }

        public CpuMetrics build() {
            return new CpuMetrics(usagePercent, userPercent, systemPercent,
                    idlePercent, iowaitPercent, loadAvg1, loadAvg5, loadAvg15, coreCount);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CpuMetrics that = (CpuMetrics) o;
        return Double.compare(that.usagePercent, usagePercent) == 0
                && Double.compare(that.userPercent, userPercent) == 0
                && Double.compare(that.systemPercent, systemPercent) == 0
                && Double.compare(that.idlePercent, idlePercent) == 0
                && Double.compare(that.iowaitPercent, iowaitPercent) == 0
                && Double.compare(that.loadAvg1, loadAvg1) == 0
                && Double.compare(that.loadAvg5, loadAvg5) == 0
                && Double.compare(that.loadAvg15, loadAvg15) == 0
                && coreCount == that.coreCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(usagePercent, userPercent, systemPercent, idlePercent,
                iowaitPercent, loadAvg1, loadAvg5, loadAvg15, coreCount);
    }

    @Override
    public String toString() {
        return "CpuMetrics{usage=" + usagePercent + "%, user=" + userPercent +
                "%, system=" + systemPercent + "%, idle=" + idlePercent +
                "%, iowait=" + iowaitPercent + "%, loadAvg=[" +
                loadAvg1 + "," + loadAvg5 + "," + loadAvg15 + "], cores=" + coreCount + '}';
    }
}
