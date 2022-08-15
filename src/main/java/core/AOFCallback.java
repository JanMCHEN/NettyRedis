package core;

import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;

import java.io.*;

public class AOFCallback implements RedisCommandCallback{
    FileOutputStream aof;
    FileDescriptor fd;
    ByteBufOutputStream writer = new ByteBufOutputStream(Unpooled.buffer());

    public AOFCallback() throws IOException {
        aof = new FileOutputStream("appendonly.aof");
        FileDescriptor fd = aof.getFD();
    }

    @Override
    public boolean support(RedisCommand cmd) {
        return true;
    }

    @Override
    public void call(RedisCommand cmd, String[] args) {

    }

    public static void main(String[] args) throws IOException {
        AOFCallback callback = new AOFCallback();
        callback.aof.write(1);
    }
}
