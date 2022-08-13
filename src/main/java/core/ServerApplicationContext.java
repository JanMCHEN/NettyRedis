package core;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class ServerApplicationContext implements CommandFactory {
    private CommandFactory factory;

    private NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private NioEventLoopGroup workerGroup = new NioEventLoopGroup(4);

    @Override
    public RedisCommand<?> getCommand(String key) {
        return factory.getCommand(key);
    }

    @Override
    public <T> RedisCommand<T> getCommand(String key, Class<T> returnType) {
        return factory.getCommand(key, returnType);
    }

    public void setFactory(CommandFactory factory) {
        this.factory = factory;
    }

    public void prepareServerBootstrap() {
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 1024);
            bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

            bootstrap.childHandler(new HandlerInit());
            bootstrap.bind(7000).sync().channel().closeFuture().sync();
        } catch (Throwable e) {
            e.printStackTrace();
        }finally {
            System.out.println("爱关不关");
        }
    }
}
