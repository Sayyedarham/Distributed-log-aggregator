package com.logaggregator.gateway.model;

import java.util.Objects;

public class AgentInfo {

    private String agentId;
    private String hostname;
    private String ipAddress;
    private String os;
    private String osVersion;
    private String agentVersion;

    public AgentInfo() {
    }

    public AgentInfo(String agentId, String hostname, String ipAddress,
                     String os, String osVersion, String agentVersion) {
        this.agentId = agentId;
        this.hostname = hostname;
        this.ipAddress = ipAddress;
        this.os = os;
        this.osVersion = osVersion;
        this.agentVersion = agentVersion;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public void setAgentVersion(String agentVersion) {
        this.agentVersion = agentVersion;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String agentId;
        private String hostname;
        private String ipAddress;
        private String os;
        private String osVersion;
        private String agentVersion;

        public Builder agentId(String agentId) {
            this.agentId = agentId;
            return this;
        }

        public Builder hostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public Builder os(String os) {
            this.os = os;
            return this;
        }

        public Builder osVersion(String osVersion) {
            this.osVersion = osVersion;
            return this;
        }

        public Builder agentVersion(String agentVersion) {
            this.agentVersion = agentVersion;
            return this;
        }

        public AgentInfo build() {
            return new AgentInfo(agentId, hostname, ipAddress, os, osVersion, agentVersion);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentInfo that = (AgentInfo) o;
        return Objects.equals(agentId, that.agentId)
                && Objects.equals(hostname, that.hostname)
                && Objects.equals(ipAddress, that.ipAddress)
                && Objects.equals(os, that.os)
                && Objects.equals(osVersion, that.osVersion)
                && Objects.equals(agentVersion, that.agentVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agentId, hostname, ipAddress, os, osVersion, agentVersion);
    }

    @Override
    public String toString() {
        return "AgentInfo{" +
                "agentId='" + agentId + '\'' +
                ", hostname='" + hostname + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", os='" + os + '\'' +
                ", osVersion='" + osVersion + '\'' +
                ", agentVersion='" + agentVersion + '\'' +
                '}';
    }
}
