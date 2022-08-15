package nio;

import io.netty.buffer.ByteBuf;
import org.junit.Test;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.*;
import java.nio.channels.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

public class NioTest {
    
    final int a = 0;
    int b;
    @Test
    public void bufferTest() {
        System.out.println(ByteOrder.nativeOrder());
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        System.out.println(buffer.put((byte) 'a'));
//        ByteBuffer.allocate(1024).put()
        System.out.println((byte) 'a');
        Float f = 2F;
        System.out.println(f);
    }

    @Test
    public void intBufferTest() {
        IntBuffer buffer = IntBuffer.allocate(1024);
        for(int i=0;i<buffer.capacity();++i) {
            buffer.put(new int[]{i});
        }
        buffer.flip();
        while(buffer.hasRemaining()) {
            System.out.println(buffer.get());
        }


    }
    public static void main(String[] args) throws InterruptedException {
        new NioTest().threadTest();
    }
    @Test
    public void channelWriteTest() throws IOException {
        String str = "hello world ";
        FileOutputStream fileOutputStream = new FileOutputStream("a.txt", true);
        FileChannel channel = fileOutputStream.getChannel();

        ByteBuffer byteBuffer = ByteBuffer.wrap(str.getBytes());
        byteBuffer.put(str.getBytes());
        byteBuffer.flip();
        channel.write(byteBuffer);
        channel.read(byteBuffer);

        channel.close();
        fileOutputStream.close();


    }

    @Test
    public void channelReadTest() throws IOException {
        FileInputStream fileInputStream = new FileInputStream("a.txt");
        FileChannel channel = fileInputStream.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(100);
        channel.read(buffer);
        System.out.println(new String(buffer.array()));
        Scanner scanner = new Scanner(System.in);
    }

    @Test
    public void transferTest() throws IOException {
        FileInputStream fileInputStream = new FileInputStream("a.txt");
        FileOutputStream fileOutputStream = new FileOutputStream("a2.txt", true);

        FileChannel channel1 = fileInputStream.getChannel();
        FileChannel channel2 = fileOutputStream.getChannel();

        channel2.transferFrom(channel1, 0, channel1.size());
        channel1.transferTo(0, channel2.size(),channel2);


    }

    @Test
    public void mapTest() throws IOException {
//        FileInputStream fileInputStream = new FileInputStream("a.txt");
        RandomAccessFile rw = new RandomAccessFile("a.txt", "rw");
        FileChannel channel = rw.getChannel();
        MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_WRITE, 0, 100);
        byte but[] = new byte[100];
        map.get(but);
        System.out.println(new String(but));
    }

    @Test
    public void socketTest() throws IOException {
        ServerSocketChannel socketChannel = ServerSocketChannel.open();
        socketChannel.bind(new InetSocketAddress(7000));

        ByteBuffer[] buffers = new ByteBuffer[3];
        for (int i = 0; i < buffers.length; ++i) {
            buffers[i] = ByteBuffer.allocate(5);
        }


        SocketChannel socketChannel1 = socketChannel.accept();
        try {
            while (true) {
                long byteRead = 0;
                while (byteRead < 12) {
                    long read = socketChannel1.read(buffers);
                    byteRead += read;
                    System.out.println(byteRead);
                    Arrays.stream(buffers).map(buffer -> "position="+buffer.position() + "," + "limit="+buffer.limit()).forEach(System.out::println);
                }

                Arrays.stream(buffers).forEach(ByteBuffer::flip);
                long byteWrite = 0;
                while (byteWrite < 12) {
                    long write = socketChannel1.write(buffers);
                    byteWrite += write;
                    System.out.println(write);


                }
                Arrays.stream(buffers).forEach(ByteBuffer::clear);
            }
        }finally {
            socketChannel.close();
    }
    }

    @Test
    public void selectorTest() throws IOException {
        ServerSocketChannel socketChannel = ServerSocketChannel.open();
        socketChannel.bind(new InetSocketAddress(7000));
        socketChannel.configureBlocking(false);
        Selector selector = Selector.open();
        socketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            if (selector.select(1000)==0) {
//                System.out.println("等待1秒无连接");
                continue;
            }
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while(iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    SocketChannel accept = socketChannel.accept();
                    accept.configureBlocking(false);
                    accept.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(100));
                }
                if (key.isReadable()) {
                    System.out.println("读取数据");
                    SocketChannel channel = (SocketChannel)key.channel();
                    ByteBuffer buffer = (ByteBuffer) key.attachment();
                    buffer.clear();
                    channel.read(buffer);
                    System.out.println(new String(buffer.array()));
                }
                iterator.remove();

            }
        }
    }

    @Test
    public void threadTest() throws InterruptedException {
        Thread thread = new Thread(() -> {
            while (true) {
                System.out.println("child+in");
                try {
                    Thread.sleep(20000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("child+out");
                    break;
                }
            }

        });
//        thread.setDaemon(false);
        thread.start();

        Thread.sleep(1000);
        System.out.println("main-stopping");
    }
}
