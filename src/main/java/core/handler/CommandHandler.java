package core.handler;

import core.*;
import core.RedisMessageFactory;
import core.exception.RedisException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.redis.*;
import io.netty.util.CharsetUtil;

import java.util.List;

public class CommandHandler extends SimpleChannelInboundHandler<Object> {
    private CommandFactory cmdFactory;
    private RedisClient client;
    private EventLoop executor;
    public void setCmdFactory(CommandFactory commands) {
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
        if(msg instanceof ArrayRedisMessage) {
            Runnable task = commandParse(ctx, (ArrayRedisMessage) msg);
            if(task!=null) {
                executor.submit(task);
            }
        }
        else if(msg instanceof InlineCommandRedisMessage) {
            ctx.writeAndFlush(msg);
        }

        else{
            throw new RedisCodecException("不支持此命令");
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

    public boolean checkArgs(ChannelHandlerContext ctx, RedisCommand cmd, String[]args, String command) {
        int stat = cmd.checkArgs(args);
        switch (stat) {
            case 0:
                return true;
            case -1:
                ctx.writeAndFlush(RedisMessageFactory.errorCommandNumberMessage(command));
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

    public Runnable commandParse(ChannelHandlerContext ctx, ArrayRedisMessage msg) {
        List<RedisMessage> commands = msg.children();
        int n = commands.size();
        if(n==0) return null;

        String cmdStr = msgToString(commands.get(0));
        RedisCommand cmd = cmdFactory.getCommand(cmdStr);
        // 命令不存在
        if(cmd==null) {
            client.setError();
            ctx.writeAndFlush(RedisMessageFactory.unknownCommandMessage(cmdStr));
            return null;
        }
        String[] args = new String[n-1];

        for(int i=1;i<n;++i) {
            args[i-1] = msgToString(commands.get(i));
        }

        // 命令参数错误
        if(!checkArgs(ctx, cmd, args, cmdStr)) {
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
            Object res=null;
            try {
                res = cmd.invoke(client, args);
            }catch (RedisException e) {
                res = e.redisMessage;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            if (res==null) res = RedisMessageFactory.NULL;
            ctx.writeAndFlush(res);
        };
    }

    public RedisClient getClient(){
        return client;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        executor.submit(()->client.close());
        ctx.close();
    }
}

