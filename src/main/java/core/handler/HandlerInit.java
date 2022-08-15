package core.handler;

import core.CommandFactory;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.redis.RedisArrayAggregator;
import io.netty.handler.codec.redis.RedisBulkStringAggregator;
import io.netty.handler.codec.redis.RedisDecoder;
import io.netty.handler.codec.redis.RedisEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class HandlerInit extends ChannelInitializer<SocketChannel> {
    private CommandFactory commandFactory;
    private EventLoop eventLoop;

    private LoggingHandler loggingHandler;
    public void setCommandFactory(CommandFactory commandFactory) {
        this.commandFactory = commandFactory;
    }

    public void setEventLoop(EventLoop eventLoop) {
        this.eventLoop = eventLoop;
    }
    public void setLoggingHandler(LoggingHandler loggingHandler) {
        this.loggingHandler = loggingHandler;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        CommandHandler cmdHandler = new CommandHandler();
        cmdHandler.setExecutor(eventLoop);
        cmdHandler.setCmdFactory(commandFactory);

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
