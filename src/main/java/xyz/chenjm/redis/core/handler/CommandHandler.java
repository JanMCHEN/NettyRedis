package xyz.chenjm.redis.core.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.redis.ArrayRedisMessage;
import io.netty.handler.codec.redis.FullBulkStringRedisMessage;
import io.netty.handler.codec.redis.InlineCommandRedisMessage;
import io.netty.handler.codec.redis.RedisMessage;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.chenjm.redis.core.RedisClient;
import xyz.chenjm.redis.core.RedisServer;

import java.util.List;
import java.util.stream.Collectors;

public class CommandHandler extends SimpleChannelInboundHandler<Object> {
    private static final Logger log = LoggerFactory.getLogger(CommandHandler.class);
    private RedisClient client;
    private RedisServer server;

    public void setServer(RedisServer server) {
        this.server = server;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        client = new RedisClient((SocketChannel) ctx.channel(), server);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        client.execute(()->client.close());
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        String[] args = resolveArgs(msg);
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

    public String[] resolveArgs(Object msg) {
        String[] args = new String[0];
        if(msg instanceof ArrayRedisMessage) {
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
        client.execute(()->client.close());
        ctx.close();
    }

}

