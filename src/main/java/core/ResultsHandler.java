package core;

import core.RedisMessagePool;
import core.RedisObject;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.redis.*;
import io.netty.util.CharsetUtil;

import java.util.LinkedList;
import java.util.List;

public class ResultsHandler extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if(msg instanceof List) {
            List<RedisMessage> res = new LinkedList<>();
            List<Object> msgL = (List<Object>) msg;
            for(var m:msgL) {
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
        RedisMessage res = null;
        if(msg==null) {
            res = RedisMessagePool.NULL;
        }
        else if (msg instanceof RedisMessage) {
            res = (RedisMessage) msg;
        }
        else if (msg instanceof RedisObject) {
            res = new FullBulkStringRedisMessage(Unpooled.copiedBuffer(msg.toString(), CharsetUtil.UTF_8));
        }
        else if (msg instanceof Boolean) {
            boolean rep = (Boolean) msg;
            if(rep) {
                res = RedisMessagePool.OK;
            }
            else {
                res = RedisMessagePool.NULL;
            }
        }
        else if (msg instanceof Long) {
            res = new IntegerRedisMessage((Long) msg);
        }
        else if (msg instanceof List) {
            List<RedisMessage> out_r = new LinkedList<>();
            List<Object> msgL = (List<Object>) msg;
            for(var m:msgL) {
                write0(ctx, m, promise, out_r);
            }
            res = new ArrayRedisMessage(out_r);
        }
        else {
            res = RedisMessagePool.ERR;
        }
        if(out==null) {
            super.write(ctx, res, promise);
        }
        else {
            out.add(res);
        }

    }
}

