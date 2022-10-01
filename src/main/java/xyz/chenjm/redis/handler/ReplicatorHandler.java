package xyz.chenjm.redis.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.redis.ErrorRedisMessage;
import io.netty.handler.codec.redis.FullBulkStringRedisMessage;
import io.netty.handler.codec.redis.SimpleStringRedisMessage;
import io.netty.util.concurrent.ScheduledFuture;
import xyz.chenjm.redis.core.RedisReplicator;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class ReplicatorHandler extends ChannelInboundHandlerAdapter {
    RedisReplicator replicator;

    ReplyState state;
    int timeout = 10;
    InetSocketAddress local;

    ScheduledFuture<?> ackFuture;


    enum ReplyState{
        PING, PONG, REPL, SYNC, RDB_REC, ACK
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof SimpleStringRedisMessage) {
            String reply = ((SimpleStringRedisMessage) msg).content();
            switch (state) {
                case PING:
                    if("PONG".equalsIgnoreCase(reply)) {
                        state = ReplyState.PONG;
                        ctx.writeAndFlush(new String[]{"replconf", "listening-port", String.valueOf(local.getPort())});
                        break;
                    }
                    throw new RuntimeException();
                case PONG:
                    if ("OK".equalsIgnoreCase(reply)) {
                        ctx.writeAndFlush(new String[]{"psync", replicator.getMasterId(), String.valueOf(replicator.getOffset())});
                        state = ReplyState.SYNC;
                        break;
                    }
                    throw new RuntimeException();
                case SYNC:
                    state = ReplyState.ACK;
                    if ("CONTINUE".equalsIgnoreCase(reply)) {
                        // 部分重同步
                        replAckTask(ctx);
                        break;
                    }
                    String[] rl = reply.split(" ");
                    if (rl.length == 3) {
                        if ("FULLRESYNC".equalsIgnoreCase(rl[0])) {
                            replicator.setMasterId(rl[1]);
                            replicator.setOffset(Long.parseLong(rl[2]));
                            state = ReplyState.RDB_REC;
                            break;
                        }
                    }
                default:
                    throw new RuntimeException();
            }
        } else if (msg instanceof FullBulkStringRedisMessage && state==ReplyState.RDB_REC) {


        } else if (msg instanceof ErrorRedisMessage) {
            ctx.close();
        }
        else {
            ctx.fireChannelRead(msg);
        }

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(new String[]{"ping"});
        state = ReplyState.PING;
        // 超时重连
        ctx.executor().schedule(()->{
            if (state == ReplyState.PING) {
                replicator.setShouldRetry(true);
                ctx.close();
            }
        }, timeout, TimeUnit.SECONDS);
        local = (InetSocketAddress)ctx.channel().localAddress();
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }

    void replAckTask(ChannelHandlerContext ctx) {
        if (ackFuture != null && !ackFuture.isCancelled()) {
            ackFuture.cancel(true);
        }
        ackFuture = ctx.executor().scheduleWithFixedDelay(() -> {
            ctx.writeAndFlush(new String[]{"replconf", "ack", String.valueOf(replicator.getOffset())});
        }, 1, 1, TimeUnit.SECONDS);
    }
}
