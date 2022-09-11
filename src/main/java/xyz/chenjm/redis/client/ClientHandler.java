package xyz.chenjm.redis.client;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.redis.ArrayRedisMessage;
import io.netty.handler.codec.redis.FullBulkStringRedisMessage;
import io.netty.handler.codec.redis.RedisMessage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class ClientHandler extends ChannelDuplexHandler {
    int readIdx, writeIdx;
    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        List<RedisMessage> args = new ArrayList<>();
        if (msg instanceof Object[]) {
            Object[] m = (Object[]) msg;
            for (Object e: m) {
                args.add(new FullBulkStringRedisMessage(Unpooled.copiedBuffer(e.toString(), StandardCharsets.US_ASCII)));
            }
        }
        else
            args.add(new FullBulkStringRedisMessage(Unpooled.copiedBuffer(msg.toString(), StandardCharsets.US_ASCII)));
        super.write(ctx, new ArrayRedisMessage(args), promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//        super.channelRead(ctx, msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }
}
