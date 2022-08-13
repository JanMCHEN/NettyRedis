package core;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.redis.RedisArrayAggregator;
import io.netty.handler.codec.redis.RedisBulkStringAggregator;
import io.netty.handler.codec.redis.RedisDecoder;
import io.netty.handler.codec.redis.RedisEncoder;

public class HandlerInit extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline
                .addLast(new RedisDecoder(true))
                .addLast(new RedisBulkStringAggregator())
                .addLast(new RedisArrayAggregator())
                .addLast(new RedisEncoder())
                .addLast(new ResultsHandler())
                .addLast(new CommandHandler());
    }
}
