import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class WriteBug {
    public static void main(String[] args) throws InterruptedException {
        NioEventLoopGroup executors = new NioEventLoopGroup(1);
        Bootstrap bootstrap = new Bootstrap();

        Channel channel = bootstrap
                .group(executors)
                .channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new ChannelDuplexHandler() {
                            @Override
                            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                                System.out.println("look at me");
                                System.out.println(msg);
                                ctx.write(msg, promise);
                            }

                        });

                    }
                }).connect("localhost", 7000).sync().channel();

        ByteBuf buf = Unpooled.directBuffer().writeBytes("get a\r\n".getBytes());

        ChannelFuture future = channel.write(buf);

        // 主线程阻塞，但并没有唤醒io线程，导致死锁；    没有唤醒是因为lazyExecute，
        // SingleThreadEventExecutor addTaskWakesUp默认false，并且immediate为false，只是将task添加到任务队列，并没有唤醒操作
        future.sync();

        channel.closeFuture().sync();

        executors.shutdownGracefully();
    }
}
