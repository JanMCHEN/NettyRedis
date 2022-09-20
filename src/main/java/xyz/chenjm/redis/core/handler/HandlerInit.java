package xyz.chenjm.redis.core.handler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.redis.RedisArrayAggregator;
import io.netty.handler.codec.redis.RedisBulkStringAggregator;
import io.netty.handler.codec.redis.RedisDecoder;
import io.netty.handler.codec.redis.RedisEncoder;
import io.netty.handler.logging.LoggingHandler;
import xyz.chenjm.redis.core.RedisServer;

public class HandlerInit extends ChannelInitializer<SocketChannel> {
    private LoggingHandler loggingHandler;
    private RedisServer server;

    public void setServer(RedisServer server) {
        this.server = server;
    }

    public void setLoggingHandler(LoggingHandler loggingHandler) {
        this.loggingHandler = loggingHandler;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        CommandHandler cmdHandler = new CommandHandler();
        cmdHandler.setServer(server);

        ChannelPipeline pipeline = ch.pipeline();
        pipeline
                .addLast("decode", new RedisDecoder(true))
                .addLast("string-decode", new RedisBulkStringAggregator())
                .addLast("array-decode", new RedisArrayAggregator());

        if(loggingHandler !=null) pipeline.addLast("logging", loggingHandler);

        pipeline
                .addLast("encode", new RedisEncoder())
                .addLast("result", new ResultsHandler())
                .addLast("command", cmdHandler)
                ;
    }
}
