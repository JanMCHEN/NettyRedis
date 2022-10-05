package xyz.chenjm.redis.codec;

import io.netty.buffer.ByteBuf;
import io.netty.util.ByteProcessor;

import java.util.ArrayList;
import java.util.List;

import static xyz.chenjm.redis.codec.AbstractRedisDecoder.DecodeType.*;

public class AbstractRedisDecoder{
    private DecodeState state;
    private DecodeType type;

    private int offset = -1;

    private InlineProcessor lineEndProcessor = new InlineProcessor();
    private IntegerInlineProcessor intEndProcessor = new IntegerInlineProcessor();

    private List<RedisMessage1> arrayMessage;


    static class InlineProcessor implements ByteProcessor {
        boolean cr = false;
        @Override
        public boolean process(byte value) throws Exception {
            if (value == '\n' && cr) {
                return false;
            }
            cr = value == '\r';
            return true;
        }

        public void reset() {
            cr = false;
        }
    }

    static class IntegerInlineProcessor extends Demo.InlineProcessor {
        int flag = 0;
        int num = 0;
        @Override
        public boolean process(byte value) throws Exception {
            if (flag == 0) {
                flag = value == '-' ? -1: 1;
                num = value - '0';
                if (num < 0 || num > 9) {
                    throw new RuntimeException();
                }
                return true;
            }
            if (!cr && value == '\r') {
                cr = true;
            } else if (cr && value == '\n') {
                return false;
            } else if (!cr && value >= '0' && value <= '9') {
                num = value - '0' + num * 10;
            }
            else {
                throw new RuntimeException();
            }
            return true;
        }

        public int getNum() {
            return num * flag;
        }

        public void reset() {
            flag = num = 0;
            cr = false;
        }
    }


    enum DecodeState {
        DECODE_TYPE, DECODE_INLINE, DECODE_NUMBER, DECODE_ARRAY, DECODE_STRING
    }


    enum DecodeType {
        SIMPLE_STRING, RAW_STRING, ERROR_STRING, INT, ARRAY(true), INLINE_CMD;
        final boolean isArray;
        DecodeType(){
            isArray = false;
        }
        DecodeType(boolean isArray) {
            this.isArray = isArray;
        }
    }

    public void decode(ByteBuf in) {

        while (in.isReadable()) {
            switch (state) {
                case DECODE_TYPE:
                    decodeType(in);
                    break;
                case DECODE_INLINE:
                    decodeInline(in);
                case DECODE_NUMBER:
                    decodeInline(in);
            }

        }

    }
    public void decodeType(ByteBuf in) {
        offset = in.readerIndex();
        byte f = in.getByte(offset);
        switch (f) {
            case '*':
                type = ARRAY;
                state = DecodeState.DECODE_NUMBER;
                break;
            case ':':
                type = INT;
                state = DecodeState.DECODE_NUMBER;
                break;
            case '+':
                type = SIMPLE_STRING;
                state = DecodeState.DECODE_INLINE;
                break;
            case '-':
                type = ERROR_STRING;
                state = DecodeState.DECODE_INLINE;
                break;
            case '$':
                type = RAW_STRING;
                state = DecodeState.DECODE_NUMBER;
                break;
            default:
                type = INLINE_CMD;
                state = DecodeState.DECODE_INLINE;
        }
        offset++;
        checkType();
    }

    protected void checkType() {

    }

    public ByteBuf decodeInline(ByteBuf in) {
        int flag = in.forEachByte(offset, in.writerIndex() - offset, lineEndProcessor);
        if (flag == -1) {
            offset = in.writerIndex();
            return null;
        }
        offset = flag + 1;
        ByteBuf read = in.slice(in.readerIndex(), offset-in.readerIndex());
        in.readerIndex(offset);
        lineEndProcessor.reset();
        state = DecodeState.DECODE_TYPE;
        return read;
    }

    public ByteBuf decodeNumber(ByteBuf in) {
        int flag = in.forEachByte(offset, in.writerIndex() - offset, intEndProcessor);
        if (flag == -1) {
            offset = in.writerIndex();
            return null;
        }
        int length = intEndProcessor.getNum();
        offset = flag + 1;
        ByteBuf read = in.slice(in.readerIndex(), offset-in.readerIndex());
        intEndProcessor.reset();
        switch (type) {
            case ARRAY:
                if (length < 0) {
                    return null;
                }
                arrayMessage = new ArrayList<>(length+1);
                arrayMessage.add(null);
                state = DecodeState.DECODE_TYPE;
                in.readerIndex(offset);
            case RAW_STRING:
                if (in.writerIndex()-offset>=length) {

                }
                break;
            default:
                // int
                in.readerIndex(offset);
                break;
        }
        return read;
    }

    private void reset() {
        state = DecodeState.DECODE_TYPE;
    }

    public void decodeString() {

    }

}
