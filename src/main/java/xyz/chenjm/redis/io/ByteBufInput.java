package xyz.chenjm.redis.io;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.io.InputStream;

public class ByteBufInput extends InputStream {
    ByteBuf buf;
    InputStream in;

    public ByteBufInput(InputStream stream) {
        in = stream;
        buf = Unpooled.directBuffer(16, 512);
    }

    @Override
    public int read() throws IOException {
        if (!buf.isReadable()) {
            buffer();
        }
        if (buf.isReadable()) {
            return buf.readByte() & 0xff;
        }
        return -1;
    }

    private void buffer() throws IOException {
        buf.writeBytes(in, buf.writableBytes());
    }

    @Override
    public void close() throws IOException {
        buf.release();
        in.close();
    }
}
