package core;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class BootstrapConfiguration{
    private int port = 7000;
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;
    private ServerBootstrap bootstrap;

    private ChannelInitializer<SocketChannel> channelInitializer = new HandlerInit();

    public BootstrapConfiguration() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(4);
        bootstrap = new ServerBootstrap();
    }

    public void afterPropertiesSet() {
        try {
            bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 1024);
            bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
            bootstrap.childHandler(channelInitializer);
            ChannelFuture closeFuture = bootstrap.bind(port).sync().channel().closeFuture();


            closeFuture.sync();
        } catch (Throwable e) {
            e.printStackTrace();
        }finally {
            destroy();
        }
    }

    public void destroy() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
