package com.logaggregator.gateway.serialization;

import com.logaggregator.gateway.model.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class BinaryCodecTest {

    @Test
    public void testServerAckEncodeDecode() {
        ServerAck ack = new ServerAck("batch_123", true, "", 1700000000000L, 5000);
        byte[] bytes = BinaryCodec.encodeServerAck(ack);

        assertNotNull(bytes);
        assertEquals(BinaryCodec.MSG_SERVER_ACK, BinaryCodec.getMessageType(bytes));
    }

    @Test
    public void testRegistrationResponseEncode() {
        List<String> metrics = new ArrayList<>();
        metrics.add("cpu");
        metrics.add("memory");
        RegistrationResponse resp = new RegistrationResponse(true, "agent-007", 10000, metrics);

        byte[] bytes = BinaryCodec.encodeRegistrationResponse(resp);
        assertNotNull(bytes);
        assertEquals(BinaryCodec.MSG_REGISTRATION_RESPONSE, BinaryCodec.getMessageType(bytes));
    }

    @Test
    public void testPayloadCodec() {
        AgentInfo info = new AgentInfo("agent-1", "host-a", "192.168.1.10", "linux", "6.1", 1000L, "1.0");
        CpuMetrics cpu = new CpuMetrics(12.5, 4.0, 83.5, 0.0, 0.0, 4, 0.5, 0.3, 0.2);
        MemoryMetrics mem = new MemoryMetrics(16000000000L, 8000000000L, 8000000000L, 50.0, 0L, 0L, 0L, 0L);

        MetricPayload payload = new MetricPayload();
        payload.setAgentInfo(info);
        payload.setCpuMetrics(cpu);
        payload.setMemoryMetrics(mem);
        payload.setTimestamp(System.currentTimeMillis());
        payload.setSequenceNumber(101);
        payload.setPriority(1);

        assertEquals("agent-1", payload.getAgentInfo().getAgentId());
        assertEquals(12.5, payload.getCpuMetrics().getUserPercent(), 0.001);
        assertEquals(50.0, payload.getMemoryMetrics().getUsedPercent(), 0.001);
    }
}

