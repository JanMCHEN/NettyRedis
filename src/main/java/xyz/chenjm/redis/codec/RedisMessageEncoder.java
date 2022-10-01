package xyz.chenjm.redis.codec;

import io.netty.buffer.ByteBuf;

import java.util.Collection;

public class RedisMessageEncoder {

    public void encode(Object message, ByteBuf buf) {
        if (message instanceof Collection) {
            Object[] array = ((Collection<?>) message).toArray();
        }
    }

}
