package com.logaggregator.gateway.pipeline;

import com.logaggregator.gateway.config.GatewayConfig;
import com.logaggregator.gateway.model.AgentInfo;
import com.logaggregator.gateway.model.LogEntry;
import com.logaggregator.gateway.model.MetricPayload;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class SanitizationStageTest {

    private SanitizationStage stage;

    @Before
    public void setup() {
        GatewayConfig.PipelineConfig config = new GatewayConfig.PipelineConfig();
        config.setMaxLogMessageLength(20);
        stage = new SanitizationStage(config);
    }

    @Test
    public void testHostnameNormalization() throws PipelineException {
        MetricPayload payload = new MetricPayload();
        payload.setAgentInfo(new AgentInfo("agent-1", "  HOST-PROD-01\u0000  ", "127.0.0.1", "linux", "6.1", 100L, "1.0"));

        stage.process(payload);
        assertEquals("host-prod-01", payload.getAgentInfo().getHostname());
    }

    @Test
    public void testLogMessageTruncation() throws PipelineException {
        MetricPayload payload = new MetricPayload();
        List<LogEntry> logs = new ArrayList<>();
        LogEntry entry = new LogEntry("syslog", "INFO", "This is an excessively long log message that needs truncating", System.currentTimeMillis());
        logs.add(entry);
        payload.setLogEntries(logs);

        stage.process(payload);
        assertTrue(entry.getMessage().contains("[TRUNCATED]"));
    }
}

