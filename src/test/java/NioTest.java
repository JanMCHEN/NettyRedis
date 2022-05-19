import io.netty.buffer.*;
import org.junit.Test;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
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

    @Test
    public void testWrite() throws IOException {
//        ByteBuf buf = Unpooled.compositeBuffer();   // direct=false;
        ByteBuf buf = new CompositeByteBuf(UnpooledByteBufAllocator.DEFAULT, true, 64);

//        for (int i=0;i<1000000000;++i) {
//            buf.writeInt(i);
//        }



        OutputStream stream = new ByteBufOutputStream(buf);

        Map<String, String> map = new HashMap<>();
        for (int i=0;i<1024*1024;++i) {
            map.put("hello"+i, "world"+i);
        }

        long tic = System.currentTimeMillis();

        ObjectOutputStream objectOutputStream = new ObjectOutputStream(stream);
        objectOutputStream.writeObject(map);
        long toc = System.currentTimeMillis();

        System.out.println("size="+buf.readableBytes()/1024/1024+"mb");
        System.out.println("cost:"+(toc-tic)/1000.+"s");
    }
}
