package xyz.chenjm.redis.codec;

import io.netty.handler.codec.redis.RedisMessage;

public interface RedisMessageDecoder1 {
    Object decode(RedisMessage msg);
}
