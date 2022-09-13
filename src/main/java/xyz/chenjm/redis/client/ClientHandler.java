package xyz.chenjm.redis.client;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.redis.ArrayRedisMessage;
import io.netty.handler.codec.redis.FullBulkStringRedisMessage;
import io.netty.handler.codec.redis.RedisMessage;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;
import xyz.chenjm.redis.codec.SimpleRedisMessageDecoder;
import xyz.chenjm.redis.codec.RedisMessageDecoder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class ClientHandler extends ChannelDuplexHandler {

    private int readIdx, writeIdx;

    private final Map<Integer, Promise<RedisMessage>> results;
    EventExecutor executor;

    RedisMessageDecoder codec = new SimpleRedisMessageDecoder();

    public ClientHandler() {
        results = new ConcurrentHashMap<>();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        executor = ctx.executor();
        super.channelActive(ctx);
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
        else if (msg instanceof String){
            String[] s = ((String) msg).split(" ");
            if (s.length > 1) {
                write(ctx, s, promise);
                return;
            }
            msg = s[0];
            args.add(new FullBulkStringRedisMessage(Unpooled.copiedBuffer(msg.toString(), StandardCharsets.US_ASCII)));
        }

        ctx.write(new ArrayRedisMessage(args), promise);
        results.put(writeIdx++, new DefaultPromise<>(ctx.executor()));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof RedisMessage)
            results.get(readIdx++).setSuccess((RedisMessage) msg);
        else
            ctx.fireChannelRead(msg);
    }

    public Object get(int id) throws ExecutionException, InterruptedException {
        Promise<RedisMessage> promise = results.get(id);
        if (promise==null)
            throw new IllegalStateException();
        RedisMessage res = promise.get();
        results.remove(id);
        return codec.decode(res);
    }

    public int getWriteIdx() {
        return writeIdx;
    }
}
