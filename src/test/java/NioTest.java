import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Queue;

public class NioTest {
    public static void main(String[] args) throws IOException {
        FileChannel open = FileChannel.open(Path.of("a.txt"), StandardOpenOption.WRITE);
        FileChannel channel = new FileInputStream("a.txt").getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(128);
        int read = -1;
        while ((read=channel.read(buffer))>0) {
            System.out.println(read);
            buffer.flip();
            open.write(buffer);
            buffer.clear();
        }

        open.close();
        channel.close();

    }
}
