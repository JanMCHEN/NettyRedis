package xyz.chenjm.redis.codec;

import io.netty.buffer.ByteBuf;

@FunctionalInterface
public interface ByteBufDecoder<T> {
    T decode(ByteBuf in);
}
