package xyz.chenjm.redis.codec;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

public class FullBulkStringMessage extends AbstractRedisMessage{
    private final int length;

    @Override
    public String toString() {
        if (length == -1)
            return null;
        if (length == 0) {
            return "";
        }
        int off = buf.writerIndex() - 2 - length;
        return buf.toString(off, length, StandardCharsets.UTF_8);
    }

    public FullBulkStringMessage(ByteBuf buf, int length) {
        this(buf, length, false);
    }

    FullBulkStringMessage(ByteBuf buf, int length, boolean unsafe) {
        this.buf = buf;
        this.length = length;
        if (!unsafe) {
            checkCode();
        }
    }

    private void checkCode(){
        int off = buf.readerIndex();
        byte b = buf.getByte(off);
        if (b == STRING_TYPE) {
            RedisMessage1.checkInteger(buf, off+1, length);
        }else {
            throw new IllegalArgumentException("should be '$', but get "+(char)b);
        }
    }


}
