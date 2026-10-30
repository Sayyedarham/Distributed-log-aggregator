package com.logaggregator.gateway.pipeline;

import com.logaggregator.gateway.config.GatewayConfig;
import com.logaggregator.gateway.model.AgentInfo;
import com.logaggregator.gateway.model.CpuMetrics;
import com.logaggregator.gateway.model.MetricPayload;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ValidationStageTest {

    private ValidationStage stage;

    @Before
    public void setup() {
        GatewayConfig.PipelineConfig config = new GatewayConfig.PipelineConfig();
        stage = new ValidationStage(config);
    }

    @Test
    public void testValidPayload() throws PipelineException {
        MetricPayload payload = new MetricPayload();
        payload.setAgentInfo(new AgentInfo("agent-1", "host-a", "127.0.0.1", "linux", "6.1", 100L, "1.0"));
        payload.setTimestamp(System.currentTimeMillis());

        MetricPayload result = stage.process(payload);
        assertNotNull(result);
    }

    @Test(expected = PipelineException.class)
    public void testMissingAgentIdThrows() throws PipelineException {
        MetricPayload payload = new MetricPayload();
        payload.setTimestamp(System.currentTimeMillis());
        stage.process(payload);
    }

    @Test
    public void testCpuClamping() throws PipelineException {
        MetricPayload payload = new MetricPayload();
        payload.setAgentInfo(new AgentInfo("agent-1", "host-a", "127.0.0.1", "linux", "6.1", 100L, "1.0"));
        payload.setTimestamp(System.currentTimeMillis());

        CpuMetrics cpu = new CpuMetrics();
        cpu.setUserPercent(150.0);
        payload.setCpuMetrics(cpu);

        stage.process(payload);
        assertEquals(100.0, payload.getCpuMetrics().getUserPercent(), 0.001);
    }
}

