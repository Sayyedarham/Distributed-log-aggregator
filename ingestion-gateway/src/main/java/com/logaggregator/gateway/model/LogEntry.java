package com.logaggregator.gateway.model;

import java.util.Objects;

public class LogEntry {

    private String level;

    private String message;

    private String logger;

    private long timestamp;

    private String threadName;

    public LogEntry() {
    }

    public LogEntry(String level, String message, String logger,
                    long timestamp, String threadName) {
        this.level = level;
        this.message = message;
        this.logger = logger;
        this.timestamp = timestamp;
        this.threadName = threadName;
    }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getLogger() { return logger; }
    public void setLogger(String logger) { this.logger = logger; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getThreadName() { return threadName; }
    public void setThreadName(String threadName) { this.threadName = threadName; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String level;
        private String message;
        private String logger;
        private long timestamp;
        private String threadName;

        public Builder level(String v) { this.level = v; return this; }
        public Builder message(String v) { this.message = v; return this; }
        public Builder logger(String v) { this.logger = v; return this; }
        public Builder timestamp(long v) { this.timestamp = v; return this; }
        public Builder threadName(String v) { this.threadName = v; return this; }

        public LogEntry build() {
            return new LogEntry(level, message, logger, timestamp, threadName);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogEntry that = (LogEntry) o;
        return timestamp == that.timestamp
                && Objects.equals(level, that.level)
                && Objects.equals(message, that.message)
                && Objects.equals(logger, that.logger)
                && Objects.equals(threadName, that.threadName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, message, logger, timestamp, threadName);
    }

    @Override
    public String toString() {
        return "LogEntry{level='" + level + "', message='" +
                (message != null && message.length() > 80 ? message.substring(0, 80) + "..." : message) +
                "', logger='" + logger + "', timestamp=" + timestamp +
                ", thread='" + threadName + "'}";
    }
}
