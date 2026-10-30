package com.logaggregator.gateway.model;

import java.util.Objects;

public class ServerAck {

    private long sequenceNumber;

    private boolean accepted;

    private String message;

    private long timestamp;

    private int errorCode;

    public ServerAck() {
    }

    public ServerAck(long sequenceNumber, boolean accepted, String message,
                     long timestamp, int errorCode) {
        this.sequenceNumber = sequenceNumber;
        this.accepted = accepted;
        this.message = message;
        this.timestamp = timestamp;
        this.errorCode = errorCode;
    }

    public static ServerAck success(long sequenceNumber) {
        return new ServerAck(sequenceNumber, true, "OK",
                System.currentTimeMillis(), 0);
    }

    public static ServerAck failure(long sequenceNumber, int errorCode, String message) {
        return new ServerAck(sequenceNumber, false, message,
                System.currentTimeMillis(), errorCode);
    }

    public long getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(long sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getErrorCode() { return errorCode; }
    public void setErrorCode(int errorCode) { this.errorCode = errorCode; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerAck that = (ServerAck) o;
        return sequenceNumber == that.sequenceNumber
                && accepted == that.accepted
                && timestamp == that.timestamp
                && errorCode == that.errorCode
                && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sequenceNumber, accepted, message, timestamp, errorCode);
    }

    @Override
    public String toString() {
        return "ServerAck{seq=" + sequenceNumber + ", accepted=" + accepted +
                ", message='" + message + "', errorCode=" + errorCode + '}';
    }
}
