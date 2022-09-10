package xyz.chenjm.redis.core.handler;

import xyz.chenjm.redis.command.RedisCommandHolder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.redis.RedisArrayAggregator;
import io.netty.handler.codec.redis.RedisBulkStringAggregator;
import io.netty.handler.codec.redis.RedisDecoder;
import io.netty.handler.codec.redis.RedisEncoder;
import io.netty.handler.logging.LoggingHandler;
import xyz.chenjm.redis.core.RedisDBFactory;

public class HandlerInit extends ChannelInitializer<SocketChannel> {
    private RedisCommandHolder commandFactory;
    private EventLoop eventLoop;
    private LoggingHandler loggingHandler;
    private RedisDBFactory dbFactory;
    public void setCommandFactory(RedisCommandHolder commandFactory) {
        this.commandFactory = commandFactory;
    }

    public void setEventLoop(EventLoop eventLoop) {
        this.eventLoop = eventLoop;
    }
    public void setLoggingHandler(LoggingHandler loggingHandler) {
        this.loggingHandler = loggingHandler;
    }

    public void setDbFactory(RedisDBFactory dbFactory) {
        this.dbFactory = dbFactory;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        CommandHandler cmdHandler = new CommandHandler();
        cmdHandler.setExecutor(eventLoop);
        cmdHandler.setCmdFactory(commandFactory);
        cmdHandler.setDbFactory(dbFactory);

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
