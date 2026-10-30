package com.logaggregator.gateway.model;

import java.util.Objects;

public class NetworkMetrics {

    private long bytesSentPerSec;

    private long bytesRecvPerSec;

    private long packetsSentPerSec;

    private long packetsRecvPerSec;

    private long errorsIn;

    private long errorsOut;

    private long dropsIn;

    private long dropsOut;

    public NetworkMetrics() {
    }

    public NetworkMetrics(long bytesSentPerSec, long bytesRecvPerSec,
                          long packetsSentPerSec, long packetsRecvPerSec,
                          long errorsIn, long errorsOut,
                          long dropsIn, long dropsOut) {
        this.bytesSentPerSec = bytesSentPerSec;
        this.bytesRecvPerSec = bytesRecvPerSec;
        this.packetsSentPerSec = packetsSentPerSec;
        this.packetsRecvPerSec = packetsRecvPerSec;
        this.errorsIn = errorsIn;
        this.errorsOut = errorsOut;
        this.dropsIn = dropsIn;
        this.dropsOut = dropsOut;
    }

    public long getBytesSentPerSec() { return bytesSentPerSec; }
    public void setBytesSentPerSec(long v) { this.bytesSentPerSec = v; }

    public long getBytesRecvPerSec() { return bytesRecvPerSec; }
    public void setBytesRecvPerSec(long v) { this.bytesRecvPerSec = v; }

    public long getPacketsSentPerSec() { return packetsSentPerSec; }
    public void setPacketsSentPerSec(long v) { this.packetsSentPerSec = v; }

    public long getPacketsRecvPerSec() { return packetsRecvPerSec; }
    public void setPacketsRecvPerSec(long v) { this.packetsRecvPerSec = v; }

    public long getErrorsIn() { return errorsIn; }
    public void setErrorsIn(long v) { this.errorsIn = v; }

    public long getErrorsOut() { return errorsOut; }
    public void setErrorsOut(long v) { this.errorsOut = v; }

    public long getDropsIn() { return dropsIn; }
    public void setDropsIn(long v) { this.dropsIn = v; }

    public long getDropsOut() { return dropsOut; }
    public void setDropsOut(long v) { this.dropsOut = v; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private long bytesSentPerSec;
        private long bytesRecvPerSec;
        private long packetsSentPerSec;
        private long packetsRecvPerSec;
        private long errorsIn;
        private long errorsOut;
        private long dropsIn;
        private long dropsOut;

        public Builder bytesSentPerSec(long v) { this.bytesSentPerSec = v; return this; }
        public Builder bytesRecvPerSec(long v) { this.bytesRecvPerSec = v; return this; }
        public Builder packetsSentPerSec(long v) { this.packetsSentPerSec = v; return this; }
        public Builder packetsRecvPerSec(long v) { this.packetsRecvPerSec = v; return this; }
        public Builder errorsIn(long v) { this.errorsIn = v; return this; }
        public Builder errorsOut(long v) { this.errorsOut = v; return this; }
        public Builder dropsIn(long v) { this.dropsIn = v; return this; }
        public Builder dropsOut(long v) { this.dropsOut = v; return this; }

        public NetworkMetrics build() {
            return new NetworkMetrics(bytesSentPerSec, bytesRecvPerSec,
                    packetsSentPerSec, packetsRecvPerSec,
                    errorsIn, errorsOut, dropsIn, dropsOut);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkMetrics that = (NetworkMetrics) o;
        return bytesSentPerSec == that.bytesSentPerSec
                && bytesRecvPerSec == that.bytesRecvPerSec
                && packetsSentPerSec == that.packetsSentPerSec
                && packetsRecvPerSec == that.packetsRecvPerSec
                && errorsIn == that.errorsIn && errorsOut == that.errorsOut
                && dropsIn == that.dropsIn && dropsOut == that.dropsOut;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bytesSentPerSec, bytesRecvPerSec, packetsSentPerSec,
                packetsRecvPerSec, errorsIn, errorsOut, dropsIn, dropsOut);
    }

    @Override
    public String toString() {
        return "NetworkMetrics{sentBps=" + bytesSentPerSec + ", recvBps=" + bytesRecvPerSec +
                ", sentPps=" + packetsSentPerSec + ", recvPps=" + packetsRecvPerSec +
                ", errIn=" + errorsIn + ", errOut=" + errorsOut +
                ", dropIn=" + dropsIn + ", dropOut=" + dropsOut + '}';
    }
}
