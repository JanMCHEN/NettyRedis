package xyz.chenjm.redis.io;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ByteBufFileOutput extends FileOutputStream {
    ByteBuf buf;

    public ByteBufFileOutput(String name) throws FileNotFoundException {
        super(name, true);
        buf = Unpooled.directBuffer(16, 512);
    }

    @Override
    public synchronized void write(int b) throws IOException {
        if (!buf.isWritable()) {
            flush();
        }
        buf.writeByte(b);
    }

    @Override
    public synchronized void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        if (len <= 0) return;
        if (len > buf.maxFastWritableBytes()) {
            flush();
        }
        int write = Math.min(len, buf.maxWritableBytes());
        buf.writeBytes(b, off, write);
        write(b, off+write, len-write);
    }

    @Override
    public void close() throws IOException {
        try {
            flush();
        } finally {
            buf.release();
            super.close();
        }
    }

    @Override
    public synchronized void flush() throws IOException {
        while (buf.isReadable()) {
            buf.readBytes(getChannel(), buf.readerIndex(), buf.readableBytes());
        }
        buf.clear();
    }
}
