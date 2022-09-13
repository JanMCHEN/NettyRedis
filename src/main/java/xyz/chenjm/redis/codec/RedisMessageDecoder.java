package xyz.chenjm.redis.codec;

import io.netty.handler.codec.redis.RedisMessage;

public interface RedisMessageDecoder {
    Object decode(RedisMessage msg);
}
