package nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

public class ChatServer {
    private final ServerSocketChannel socketChannel;
    private final Selector selector;

    private ChatServer(int port) throws IOException {
        socketChannel = ServerSocketChannel.open();
        socketChannel.bind(new InetSocketAddress(port));
        socketChannel.configureBlocking(false);
        selector = Selector.open();
        socketChannel.register(selector, SelectionKey.OP_ACCEPT);

    }

    private void start() throws IOException {
        int i= 0;
        while(true) {
            System.out.println("select "+ ++i +"times");
            selector.select();
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    SocketChannel accept = socketChannel.accept();
                    System.out.println(accept.getRemoteAddress().toString()+"已连接");
                    accept.configureBlocking(false);
                    accept.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(100));
                }
                if (key.isReadable()) {
                    System.out.println("读取数据");
                    SocketChannel channel = (SocketChannel)key.channel();
                    ByteBuffer buffer = (ByteBuffer) key.attachment();
                    try {
                        int read = channel.read(buffer);
                        if (read==-1) throw new IOException("不是空轮询bug, linux断开连接不会抛异常，并且有持续的可读事件    ");
                    } catch (IOException e) {
                        System.out.println(channel.getRemoteAddress().toString()+"断开连接");
                        System.out.println(e.getMessage());
                        key.cancel();
                        iterator.remove();
                        continue;
                    }
                    buffer.flip();

                    for(SelectionKey sk:selector.keys()) {
                        Channel channelWt = sk.channel();
                        buffer.mark();
                        if (channelWt instanceof SocketChannel && channelWt != channel)
                            ((SocketChannel)channelWt).write(buffer);
                        buffer.reset();
                    }
                    byte []msg = new byte[buffer.limit()-buffer.position()];
                    buffer.get(msg, buffer.position(), msg.length);
                    System.out.println(new String(msg));
                    buffer.clear();
                }

                iterator.remove();
            }
        }

    }

    public static void run() {
        try {
            ChatServer server = new ChatServer(7000);
            server.start();
        } catch (IOException e) {
            System.out.println("服务端启动失败");
            e.printStackTrace();
            return;
        }
        int i = 0;

    }

    public static void main(String[] args) {
        run();
    }


}
