package com.logaggregator.gateway.pipeline;

import com.logaggregator.gateway.config.GatewayConfig;
import com.logaggregator.gateway.model.AgentInfo;
import com.logaggregator.gateway.model.LogEntry;
import com.logaggregator.gateway.model.MetricPayload;

import java.util.List;

public class SanitizationStage extends PipelineStage {

    private final int maxLogMessageLength;

    public SanitizationStage(GatewayConfig.PipelineConfig config) {
        this.maxLogMessageLength = config.getMaxLogMessageLength();
    }

    @Override
    public String getName() {
        return "SanitizationStage";
    }

    @Override
    public MetricPayload process(MetricPayload payload) throws PipelineException {
        AgentInfo info = payload.getAgentInfo();
        if (info != null) {
            if (info.getHostname() != null) {

                String cleanHost = info.getHostname().replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "").trim().toLowerCase();
                info.setHostname(cleanHost);
            }
        }

        List<LogEntry> logs = payload.getLogEntries();
        if (logs != null) {
            for (LogEntry entry : logs) {
                if (entry.getMessage() != null) {
                    String msg = entry.getMessage().replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
                    if (msg.length() > maxLogMessageLength) {
                        msg = msg.substring(0, maxLogMessageLength) + "... [TRUNCATED]";
                    }
                    entry.setMessage(msg);
                }
            }
        }

        return payload;
    }
}

