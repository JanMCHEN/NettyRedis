package xyz.chenjm.redis.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class RedisMessageDecoderTest {

    @Test
    public void testSimple() {
        RedisMessageDecoder.SimpleMessageDecoder decoder = new RedisMessageDecoder.SimpleMessageDecoder();

        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes("+ashhsahh\r\n".getBytes(StandardCharsets.UTF_8));

        System.out.println(decoder.decode(buf));

        System.out.println(buf);
    }

    @Test
    public void testString() {
        FullBulkStringMessageDecoder decoder = new FullBulkStringMessageDecoder();
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes("$8\r\nashhsahh\r\n".getBytes(StandardCharsets.UTF_8));

        System.out.println(decoder.decode(buf));

        System.out.println(buf);
    }

}