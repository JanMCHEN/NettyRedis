package core.handler;

import core.*;
import core.RedisMessageFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.redis.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class CommandHandler extends SimpleChannelInboundHandler<Object> {
    private static final Logger log = LoggerFactory.getLogger(CommandHandler.class);
    private RedisCommandHolder cmdFactory;
    private RedisClient client;
    private EventLoop executor;

    public void setCmdFactory(RedisCommandHolder commands) {
        cmdFactory = commands;
    }
    public void setExecutor(EventLoop loop) {
        executor = loop;
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        client = new RedisClient(ctx);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        executor.submit(()->client.close());
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        String[] args = resolveArgs(msg);
        if (args.length==0) {
            ctx.writeAndFlush(RedisMessageFactory.ERR);
            return;
        }
        Runnable task = commandParse(ctx, args);
        if(task != null) {
            executor.submit(task);
        }
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

    public boolean checkArgs(ChannelHandlerContext ctx, RedisCommand cmd, String[]args) {
        int stat = cmd.checkArgs(args);
        switch (stat) {
            case 0:
                return true;
            case -1:
                ctx.writeAndFlush(RedisMessageFactory.errorCommandNumberMessage(args[0]));
                break;
            case -2:
                ctx.writeAndFlush(RedisMessageFactory.ERR_SYNTAX);
                break;
            case -3:
                ctx.writeAndFlush(RedisMessageFactory.ERR_INT);
                break;
            default:
                break;
        }
        return false;
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

    public Runnable commandParse(ChannelHandlerContext ctx, String[] args) {
        if(args.length==0) return null;
        RedisCommand cmd = cmdFactory.getCommand(args[0]);
        // 命令不存在
        if(cmd==null) {
            client.setError();
            ctx.writeAndFlush(RedisMessageFactory.unknownCommandMessage(args[0]));
            return null;
        }

        // 命令参数错误
        if(!checkArgs(ctx, cmd, args)) {
            client.setError();
            return null;
        }

        // 事务命令
        if(client.isMulti() && !cmd.isMultiProcess()) {
            client.addCommand(cmd, args);
            ctx.writeAndFlush(RedisMessageFactory.QUEUED);
            return null;
        }

        return () -> {
            Object res = cmdFactory.call(cmd, client, args);
            if (res==null) res = RedisMessageFactory.NULL;
            ctx.writeAndFlush(res);
        };
    }

    public RedisClient getClient(){
        return client;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("closed because:", cause);
        executor.submit(()->client.close());
        ctx.close();
    }

}

