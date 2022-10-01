package xyz.chenjm.redis.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.redis.ArrayRedisMessage;
import io.netty.handler.codec.redis.FullBulkStringRedisMessage;
import io.netty.handler.codec.redis.InlineCommandRedisMessage;
import io.netty.handler.codec.redis.RedisMessage;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.chenjm.redis.core.RedisClient;
import xyz.chenjm.redis.core.RedisServer;

import java.util.List;
import java.util.stream.Collectors;

public class RedisCommandHandler extends ChannelDuplexHandler {
    private static final Logger log = LoggerFactory.getLogger(RedisCommandHandler.class);
    private RedisClient client;
    private RedisServer server;

    public void setServer(RedisServer server) {
        this.server = server;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        client = new RedisClient(ctx.channel(), server);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        client.close();
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        String[] args = resolveArgs(msg);
        if (args==null) {
            ctx.fireChannelRead(msg);
            return;
        }
        ReferenceCountUtil.release(msg);
        client.execute(args);
    }

    public String msgToString(RedisMessage msg) {
        if(msg instanceof FullBulkStringRedisMessage) {
            return ((FullBulkStringRedisMessage)msg).content().toString(CharsetUtil.UTF_8);
        }
        return null;
    }
    public byte[] getBytes(RedisMessage msg) {
        if(msg instanceof FullBulkStringRedisMessage) {
            ByteBuf content = ((FullBulkStringRedisMessage) msg).content();
            byte [] ans = new byte[content.readableBytes()];
            content.readBytes(ans);
            return ans;
        }
        return null;
    }

    private String[] resolveArgs(Object msg) {
        String[] args = null;
        if(msg instanceof ArrayRedisMessage) {
            args = new String[0];
            List<RedisMessage> content = ((ArrayRedisMessage) msg).children();
            args = content.stream().map(this::msgToString).collect(Collectors.toList()).toArray(args);
        } else if (msg instanceof InlineCommandRedisMessage) {
            String content = ((InlineCommandRedisMessage) msg).content();
            args = content.split(" ");
        }
        return args;
    }

    public RedisClient getClient(){
        return client;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("closed: ", cause);
        ctx.close();
    }

}

