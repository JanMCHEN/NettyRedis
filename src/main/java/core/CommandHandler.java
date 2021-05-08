package core;

import bin.Server;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.redis.*;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;

import java.util.List;

public class CommandHandler extends SimpleChannelInboundHandler {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if(msg instanceof ArrayRedisMessage) {
            Runnable task = commandParse(ctx, (ArrayRedisMessage) msg);
            if(task!=null) {
                Server.processGroup.submit(task);
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
            FullBulkStringRedisMessage msg1 = (FullBulkStringRedisMessage) msg;
            return msg1.content().toString(CharsetUtil.UTF_8);
        }
        return null;
    }

    public boolean checkArgs(ChannelHandlerContext ctx, CommandMap.AbstractCommand com, String []args, String command) {
        int stat = com.checkArgs(args);
        switch (stat) {
            case 0:
                return true;
            case -1:
                ctx.writeAndFlush(new ErrorRedisMessage("ERR wrong number of arguments for '"+command+ "' command"));
                break;
            case -2:
                ctx.writeAndFlush(RedisMessagePool.ERR_SYNTAX);
                break;
            case -3:
                ctx.writeAndFlush(RedisMessagePool.ERR_INT);
                break;
            default:
                break;
        }
        return false;
    }

    public Runnable commandParse(ChannelHandlerContext ctx, ArrayRedisMessage msg) {
        List<RedisMessage> commands = msg.children();
        RedisClient client = (RedisClient)ctx.channel().attr(AttributeKey.valueOf("client")).get();
        int n = commands.size();
        if(n==0) {
            return null;
        }
        String comStr = msgToString(commands.get(0));
        CommandMap.AbstractCommand com = CommandMap.getInstance().get(comStr);
        if(com==null) {
            client.setError();
            ctx.writeAndFlush(new ErrorRedisMessage("ERR unknown command '"+comStr+"'"));
            return null;
        }
        String[] args = new String[n-1];

        for(int i=1;i<n;++i) {
            args[i-1] = msgToString(commands.get(i));
        }

        if(!checkArgs(ctx, com, args, comStr)) {
            client.setError();
            return null;
        };

        if(client.isMulti() && !com.isMultiProcess()) {
            client.addCommand(com, args);
            ctx.writeAndFlush(RedisMessagePool.QUEUED);
            return null;
        }
        return () -> {
            Object res;
            try {
                res = com.invoke(client, args);
                if(res==null) {
                    res = RedisMessagePool.NULL;
                }
                else if(res.equals(-1L)) {
                    res = RedisMessagePool.ERR_TYPE;
                }
                ctx.writeAndFlush(res);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        };
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}

