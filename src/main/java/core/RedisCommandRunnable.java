package core;

import core.exception.RedisException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

public class RedisCommandRunnable implements Runnable{
    private RedisCommand cmd;
    String[] args;
    RedisCommandCallback callback;
    ChannelHandlerContext ctx;
    private RedisClient client;

    public RedisCommandRunnable(RedisCommand cmd, String[] args, RedisCommandCallback callback, ChannelHandlerContext ctx, RedisClient client) {
        this.cmd = cmd;
        this.args = args;
        this.callback = callback;
        this.ctx = ctx;
        this.client = client;
    }

    @Override
    public void run() {
        Object res=null;
        try {
            res = cmd.invoke(client, args);
            ByteBuf cmdBuf = (ByteBuf) ctx.channel().attr(AttributeKey.valueOf("cmd")).get();
            if(callback!=null) {
                if(callback.support(cmd)) {
                    callback.call(cmd, cmdBuf);
                }
            }
        }catch (RedisException e) {
            res = e.redisMessage;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        if (res==null) res = RedisMessageFactory.NULL;

        ctx.writeAndFlush(res);
    }
}
