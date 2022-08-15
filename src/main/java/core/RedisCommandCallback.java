package core;

import io.netty.buffer.ByteBuf;

import java.io.IOException;

public interface RedisCommandCallback{
    boolean support(RedisCommand cmd);
    void call(RedisCommand cmd, ByteBuf args) throws IOException;
}
