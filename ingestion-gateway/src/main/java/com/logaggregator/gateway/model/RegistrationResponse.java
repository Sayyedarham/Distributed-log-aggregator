package com.logaggregator.gateway.model;

import java.util.Objects;

public class RegistrationResponse {

    private boolean accepted;

    private String message;

    private String sessionId;

    private int recommendedIntervalSec;

    private long timestamp;

    public RegistrationResponse() {
    }

    public RegistrationResponse(boolean accepted, String message, String sessionId,
                                int recommendedIntervalSec, long timestamp) {
        this.accepted = accepted;
        this.message = message;
        this.sessionId = sessionId;
        this.recommendedIntervalSec = recommendedIntervalSec;
        this.timestamp = timestamp;
    }

    public static RegistrationResponse success(String sessionId) {
        return new RegistrationResponse(true, "Registration accepted",
                sessionId, 0, System.currentTimeMillis());
    }

    public static RegistrationResponse rejected(String reason) {
        return new RegistrationResponse(false, reason, null, 0,
                System.currentTimeMillis());
    }

    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public int getRecommendedIntervalSec() { return recommendedIntervalSec; }
    public void setRecommendedIntervalSec(int recommendedIntervalSec) { this.recommendedIntervalSec = recommendedIntervalSec; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegistrationResponse that = (RegistrationResponse) o;
        return accepted == that.accepted
                && recommendedIntervalSec == that.recommendedIntervalSec
                && timestamp == that.timestamp
                && Objects.equals(message, that.message)
                && Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accepted, message, sessionId, recommendedIntervalSec, timestamp);
    }

    @Override
    public String toString() {
        return "RegistrationResponse{accepted=" + accepted + ", message='" + message +
                "', sessionId='" + sessionId + "', recommendedInterval=" +
                recommendedIntervalSec + "s}";
    }
}
