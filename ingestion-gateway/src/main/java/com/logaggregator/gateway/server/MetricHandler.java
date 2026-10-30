package com.logaggregator.gateway.server;

import com.logaggregator.gateway.events.EventBus;
import com.logaggregator.gateway.events.EventType;
import com.logaggregator.gateway.events.SystemEvent;
import com.logaggregator.gateway.metrics.GatewayMetrics;
import com.logaggregator.gateway.model.*;
import com.logaggregator.gateway.pipeline.Pipeline;
import com.logaggregator.gateway.serialization.BinaryCodec;
import com.logaggregator.gateway.state.AgentRegistry;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetricHandler extends SimpleChannelInboundHandler<byte[]> {

    private static final Logger logger = LoggerFactory.getLogger(MetricHandler.class);

    private final Pipeline pipeline;
    private final AgentRegistry agentRegistry;
    private final GatewayMetrics gatewayMetrics;
    private final EventBus eventBus;

    public MetricHandler(
            Pipeline pipeline,
            AgentRegistry agentRegistry,
            GatewayMetrics gatewayMetrics,
            EventBus eventBus
    ) {
        this.pipeline = pipeline;
        this.agentRegistry = agentRegistry;
        this.gatewayMetrics = gatewayMetrics;
        this.eventBus = eventBus;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        gatewayMetrics.incrementConnections();
        logger.debug("Client TCP connection established from {}", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        gatewayMetrics.decrementConnections();
        logger.debug("Client TCP connection closed from {}", ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, byte[] msg) throws Exception {
        long tStart = System.nanoTime();
        gatewayMetrics.recordMessage(msg.length);

        byte msgType = BinaryCodec.getMessageType(msg);

        switch (msgType) {
            case BinaryCodec.MSG_AGENT_REGISTRATION:
                handleRegistration(ctx, msg);
                break;

            case BinaryCodec.MSG_METRIC_BATCH:
                handleBatch(ctx, msg);
                break;

            case BinaryCodec.MSG_METRIC_PAYLOAD:
                handleSinglePayload(ctx, msg);
                break;

            default:
                logger.warn("Received unrecognized message type byte: {}", msgType);
                break;
        }

        double latencyMs = (System.nanoTime() - tStart) / 1_000_000.0;
        gatewayMetrics.recordLatency(latencyMs);
    }

    private void handleRegistration(ChannelHandlerContext ctx, byte[] data) {
        try {
            AgentRegistration reg = BinaryCodec.decodeRegistration(data);
            AgentInfo info = reg.getAgentInfo();
            logger.info("Handling registration for agent {} (hostname: {}, os: {})",
                    info.getAgentId(), info.getHostname(), info.getOsType());

            agentRegistry.register(info);

            if (eventBus != null) {
                Map<String, Object> evData = new HashMap<>();
                evData.put("hostname", info.getHostname());
                eventBus.publish(new SystemEvent(EventType.AGENT_REGISTERED, info.getAgentId(), evData));
            }

            List<String> metrics = new ArrayList<>();
            metrics.add("cpu");
            metrics.add("memory");
            metrics.add("disk");
            metrics.add("network");

            RegistrationResponse resp = new RegistrationResponse(
                    true,
                    info.getAgentId(),
                    10000,
                    metrics
            );

            byte[] respBytes = BinaryCodec.encodeRegistrationResponse(resp);
            ctx.writeAndFlush(respBytes);
        } catch (Exception e) {
            logger.error("Error processing agent registration: {}", e.getMessage(), e);
        }
    }

    private void handleBatch(ChannelHandlerContext ctx, byte[] data) {
        MetricBatch batch = null;
        try {
            batch = BinaryCodec.decodeBatch(data);
            gatewayMetrics.recordBatch();

            String agentId = batch.getAgentId();
            List<MetricPayload> payloads = batch.getPayloads();

            for (MetricPayload p : payloads) {
                pipeline.execute(p);
                if (p.getAgentInfo() != null) {
                    agentRegistry.recordActivity(p.getAgentInfo().getAgentId(), p.getSequenceNumber());
                }
            }

            ServerAck ack = new ServerAck(batch.getBatchId(), true, "", System.currentTimeMillis(), 10000);
            byte[] ackBytes = BinaryCodec.encodeServerAck(ack);
            ctx.writeAndFlush(ackBytes);

        } catch (Exception e) {
            gatewayMetrics.recordError();
            logger.error("Error processing metric batch: {}", e.getMessage(), e);

            String batchId = batch != null ? batch.getBatchId() : "unknown";
            ServerAck nack = new ServerAck(batchId, false, e.getMessage(), System.currentTimeMillis(), 10000);
            byte[] nackBytes = BinaryCodec.encodeServerAck(nack);
            ctx.writeAndFlush(nackBytes);
        }
    }

    private void handleSinglePayload(ChannelHandlerContext ctx, byte[] data) {
        try {
            MetricPayload payload = BinaryCodec.decodePayload(data);
            pipeline.execute(payload);
            if (payload.getAgentInfo() != null) {
                agentRegistry.recordActivity(payload.getAgentInfo().getAgentId(), payload.getSequenceNumber());
            }
        } catch (Exception e) {
            gatewayMetrics.recordError();
            logger.error("Error processing metric payload: {}", e.getMessage(), e);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Exception in Netty channel {}: {}", ctx.channel().remoteAddress(), cause.getMessage());
        ctx.close();
    }
}

