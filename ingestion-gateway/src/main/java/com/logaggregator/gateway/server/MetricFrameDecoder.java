package com.logaggregator.gateway.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class MetricFrameDecoder extends LengthFieldBasedFrameDecoder {

    public MetricFrameDecoder(int maxFrameSizeBytes) {
        super(
                maxFrameSizeBytes,
                0,
                4,
                0,
                4
        );
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        }

        byte[] payloadBytes = new byte[frame.readableBytes()];
        frame.readBytes(payloadBytes);
        frame.release();
        return payloadBytes;
    }
}

