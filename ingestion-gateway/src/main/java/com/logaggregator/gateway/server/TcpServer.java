package com.logaggregator.gateway.server;

import com.logaggregator.gateway.config.GatewayConfig;
import com.logaggregator.gateway.events.EventBus;
import com.logaggregator.gateway.metrics.GatewayMetrics;
import com.logaggregator.gateway.pipeline.Pipeline;
import com.logaggregator.gateway.state.AgentRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TcpServer {

    private static final Logger logger = LoggerFactory.getLogger(TcpServer.class);

    private final GatewayConfig.ServerConfig config;
    private final Pipeline pipeline;
    private final AgentRegistry agentRegistry;
    private final GatewayMetrics gatewayMetrics;
    private final EventBus eventBus;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public TcpServer(
            GatewayConfig.ServerConfig config,
            Pipeline pipeline,
            AgentRegistry agentRegistry,
            GatewayMetrics gatewayMetrics,
            EventBus eventBus
    ) {
        this.config = config;
        this.pipeline = pipeline;
        this.agentRegistry = agentRegistry;
        this.gatewayMetrics = gatewayMetrics;
        this.eventBus = eventBus;
    }

    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(config.getBossThreads());
        workerGroup = new NioEventLoopGroup(config.getWorkerThreads());

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();

                            p.addLast("frameDecoder", new MetricFrameDecoder(config.getMaxFrameSizeBytes()));
                            p.addLast("frameEncoder", new MetricFrameEncoder());

                            p.addLast("metricHandler", new MetricHandler(
                                    pipeline, agentRegistry, gatewayMetrics, eventBus
                            ));
                        }
                    });

            ChannelFuture f = b.bind(config.getHost(), config.getPort()).sync();
            serverChannel = f.channel();
            logger.info("Ingestion Gateway TCP Server listening on {}:{}", config.getHost(), config.getPort());
        } catch (Exception e) {
            logger.error("Failed to start TCP server on port {}: {}", config.getPort(), e.getMessage());
            stop();
            throw e;
        }
    }

    public void stop() {
        logger.info("Stopping Ingestion Gateway TCP Server...");
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        logger.info("TCP Server stopped.");
    }
}

