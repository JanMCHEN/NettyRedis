package xyz.chenjm.redis.codec;

import io.netty.buffer.ByteBuf;

public class FullBulkStringMessageDecoder implements RedisMessageDecoder<FullBulkStringMessage> {
    int offset = -1;
    IntegerInlineProcessor findInteger = new IntegerInlineProcessor();
    int length = -2;

    @Override
    public FullBulkStringMessage decode(ByteBuf in) {
        if (offset == -1) {
            offset = in.readerIndex();
            if (in.getByte(offset) != RedisMessage1.STRING_TYPE) {
                throw new RuntimeException();
            }
            offset++;
        } else if (length >= 0) {
            if (in.writerIndex() - offset < length + 2) {
                return null;
            }
            offset += length + 2;
            int reader = in.readerIndex();
            in.readerIndex(offset);
            ByteBuf slice = in.slice(reader, offset - reader);
            in.readerIndex(offset);
            return new FullBulkStringMessage(slice, length, true);
        }

        int idx = in.forEachByte(offset, in.writerIndex() - offset, findInteger);
        if (idx == -1) {
            offset = in.writerIndex();
            return null;
        }
        length = findInteger.getNum();
        offset = idx + 1;

        if (length == -1) {
            int st = in.readerIndex();
            in.readerIndex(offset);
            return new FullBulkStringMessage(in.slice(st, offset - st), -1, true);
        }
        if (length < -1) {
            throw new RuntimeException();
        }
        return decode(in);
    }

    @Override
    public void reset() {
        offset = -1;
        length = -2;
        findInteger.reset();
    }

    @Override
    public boolean support(ByteBuf in) {
        return in.getByte(in.readerIndex()) == RedisMessage1.INT_TYPE;
    }
}
