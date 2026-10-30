package com.logaggregator.gateway.state;

import com.logaggregator.gateway.model.AgentInfo;
import org.junit.Test;

import static org.junit.Assert.*;

public class AgentRegistryTest {

    @Test
    public void testRegisterAndRecordActivity() {
        AgentRegistry registry = new AgentRegistry();
        AgentInfo info = new AgentInfo("node-42", "prod-worker-42", "10.0.0.42", "linux", "5.15", 1000L, "1.0");

        assertFalse(registry.isRegistered("node-42"));
        registry.register(info);
        assertTrue(registry.isRegistered("node-42"));

        AgentState state = registry.getAgent("node-42");
        assertNotNull(state);
        assertEquals(0, state.getTotalPayloadsReceived());

        registry.recordActivity("node-42", 1);
        registry.recordActivity("node-42", 2);

        assertEquals(2, state.getTotalPayloadsReceived());
        assertEquals(2, state.getLastSequenceNumber());
        assertEquals(1, registry.getActiveCount());
    }
}

