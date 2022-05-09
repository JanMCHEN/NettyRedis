package nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class ChatClient {

    SocketChannel socketChannel;

    private ChatClient() throws IOException {
        socketChannel = SocketChannel.open();
        int PORT = 7000;
        String HOST = "127.0.0.1";
        socketChannel.connect(new InetSocketAddress(HOST, PORT));
    }

    private void read(ByteBuffer buffer) throws IOException {
        socketChannel.read(buffer);
    }

    private void write(String str) throws IOException {
        socketChannel.write(ByteBuffer.wrap(str.getBytes()));
    }

    public static void run() throws IOException {
        ChatClient client = new ChatClient();
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        new Thread(()-> {
            while(true){
                System.out.println("开始接受数据");
            try {
                client.read(buffer);
                System.out.println(new String(buffer.array()));
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }finally {
                buffer.clear();
            }
            }
        }).start();

        Scanner scanner = new Scanner(System.in);
        while (true) {

            if(scanner.hasNextLine()) {
                String str = scanner.nextLine() + "\r\n";
                client.write(str);
            }
        }
    }

    public static void main(String[] args) {
        try {
            run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}
