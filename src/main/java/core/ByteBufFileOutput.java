package core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

class ByteBufFileOutput extends FileOutputStream {
    ByteBuf buf;

    public ByteBufFileOutput(String name) throws FileNotFoundException {
        super(name, true);
        buf = Unpooled.directBuffer(16, 512);
    }

    @Override
    public void write(int b) throws IOException {
        if (!buf.isWritable()) {
            flush();
        }
        buf.writeByte(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        if (!buf.isWritable()) {
            flush();
        }
        buf.writeBytes(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (!buf.isWritable()) {
            flush();
        }
        buf.writeBytes(b, off, len);
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
    public void flush() throws IOException {
        while (buf.isReadable()) {
            buf.readBytes(getChannel(), buf.readerIndex(), buf.readableBytes());
        }
        super.flush();
        buf.clear();
    }

    public void fsync() {
        try {
            getFD().sync();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
