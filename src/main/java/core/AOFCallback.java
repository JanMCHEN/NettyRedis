package core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class AOFCallback implements RedisCommandCallback{
    FileOutputStream aof;
    FileChannel fileChannel;
    FileDescriptor fd;
    ByteBufOutputStream writer = new ByteBufOutputStream(Unpooled.buffer());

    public AOFCallback() throws IOException {
        aof = new FileOutputStream("appendonly.aof", true);
        fileChannel = aof.getChannel();
    }

    @Override
    public boolean support(RedisCommand cmd) {
        return true;
    }

    @Override
    public void call(RedisCommand cmd, ByteBuf buf) throws IOException {
        int readerIndex = buf.readerIndex();
        buf.resetReaderIndex();
        buf.readBytes(fileChannel, readerIndex);
    }

    public void fsync() {
        try {
            fileChannel.force(false);
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            fileChannel.close();
            aof.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        AOFCallback callback = new AOFCallback();
        FileChannel channel = callback.aof.getChannel();
        ByteBuf byteBuf = Unpooled.wrappedBuffer("aaa".getBytes());
        System.out.println(byteBuf);
        ByteBuffer buffer = byteBuf.internalNioBuffer(byteBuf.readerIndex(), byteBuf.readableBytes());
        channel.write(buffer);
        buffer = byteBuf.internalNioBuffer(byteBuf.readerIndex(), byteBuf.readableBytes());
        channel.write(buffer);
        System.out.println(byteBuf);
    }
}
