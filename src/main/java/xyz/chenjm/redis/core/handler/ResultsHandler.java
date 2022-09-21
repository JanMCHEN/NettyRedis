package xyz.chenjm.redis.core.handler;

import xyz.chenjm.redis.core.RedisMessageFactory;
import xyz.chenjm.redis.core.structure.RedisString;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.redis.*;

import java.util.LinkedList;
import java.util.List;

public class ResultsHandler extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if(msg instanceof Object[]) {
            List<RedisMessage> res = new LinkedList<>();
            for(Object m:(Object[])msg) {
                write0(ctx, m, promise, res);
            }
            super.write(ctx, new ArrayRedisMessage(res), promise);
        }
        else if(msg instanceof RedisMessage) {
            super.write(ctx, msg, promise);
        }
        else{
            write0(ctx, msg, promise, null);
        }
    }
    public void write0(ChannelHandlerContext ctx, Object msg, ChannelPromise promise, List<RedisMessage> out) throws Exception {
        RedisMessage res;
        if(msg==null) {
            res = RedisMessageFactory.NULL;
        }
        else if (msg instanceof RedisMessage) {
            res = (RedisMessage) msg;
        }
        else if (msg instanceof RedisString) {
            res = new FullBulkStringRedisMessage(Unpooled.copiedBuffer(((RedisString) msg).getBytes()));
//            res = new FullBulkStringRedisMessage(Unpooled.copiedBuffer(msg.toString(), CharsetUtil.UTF_8));
        }
        else if (msg instanceof Boolean) {
            res = (boolean)msg?RedisMessageFactory.OK: RedisMessageFactory.NULL;
        }
        else if (msg instanceof Long) {
            res = new IntegerRedisMessage((Long) msg);
        }
        else if (msg instanceof Object[]) {
            List<RedisMessage> out_r = new LinkedList<>();
            for(Object m:(Object[])msg) {
                write0(ctx, m, promise, out_r);
            }
            res = new ArrayRedisMessage(out_r);
        }
        else if(msg instanceof Runnable) {
            ((Runnable) msg).run();
            res = RedisMessageFactory.OK;
        }
        else {
            res = RedisMessageFactory.ERR;
        }
        if(out==null) {
            super.write(ctx, res, promise);
        }
        else {
            out.add(res);
        }

    }
}
