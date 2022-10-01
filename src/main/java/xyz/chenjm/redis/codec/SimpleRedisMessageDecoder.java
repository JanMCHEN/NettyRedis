package xyz.chenjm.redis.codec;

import io.netty.handler.codec.redis.ArrayRedisMessage;
import io.netty.handler.codec.redis.FullBulkStringRedisMessage;
import io.netty.handler.codec.redis.IntegerRedisMessage;
import io.netty.handler.codec.redis.RedisMessage;
import io.netty.util.ReferenceCountUtil;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimpleRedisMessageDecoder implements RedisMessageDecoder1 {
    boolean release = true;
    Charset charSet = StandardCharsets.UTF_8;

    @Override
    public Object decode(RedisMessage msg) {
        Object ans;
        if (msg instanceof ArrayRedisMessage) {
            ans = decodeArray((ArrayRedisMessage) msg);
        } else if (msg instanceof FullBulkStringRedisMessage) {
            ans = decodeString((FullBulkStringRedisMessage) msg);
        } else if (msg instanceof IntegerRedisMessage) {
            ans = decodeInteger((IntegerRedisMessage) msg);
        } else {
            ans = msg.toString();
            ReferenceCountUtil.release(msg);
        }
        return ans;
    }

    List<Object> decodeArray(ArrayRedisMessage msg) {
        if (msg.isNull())
            return null;
        List<RedisMessage> children = msg.children();
        if (children.isEmpty())
            return Collections.emptyList();

        List<Object> ans = new ArrayList<>(children.size());
        for (RedisMessage m: children) {
            ans.add(decode(m));
        }
        if (release)
            msg.release();

        return ans;
    }

    String decodeString(FullBulkStringRedisMessage msg) {
        if (msg.isNull())
            return null;
        String s = msg.content().toString(charSet);
        if (release)
            msg.release();
        return s;
    }

    long decodeInteger(IntegerRedisMessage msg) {
        return msg.value();
    }
}
