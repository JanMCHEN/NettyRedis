package xyz.chenjm.redis.codec;

import io.netty.buffer.ByteBuf;
import io.netty.util.ByteProcessor;

public class Demo {
    private DecodeState state;
    private DecodeType type;

    private InlineProcessor lineEndProcessor = new InlineProcessor();
    private IntegerInlineProcessor intEndProcessor = new IntegerInlineProcessor();


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

    static class IntegerInlineProcessor extends InlineProcessor {
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
        DECODE_TYPE, DECODE_INLINE, DECODE_NUMBER, DECODE_ARRAY
    }

    enum DecodeType {
        SIMPLE_STRING, RAW_STRING, ERROR_STRING, NUMBER, ARRAY(true), INLINE_CMD;
        final boolean isArray;
        DecodeType(){
            isArray = false;
        }
        DecodeType(boolean isArray) {
            this.isArray = isArray;
        }
    }

    public void decodeType(byte f) {
        switch (f) {
            case '*':
                type = DecodeType.ARRAY;
                break;
            case ':':
                type = DecodeType.NUMBER;
                break;
            case '+':
                type = DecodeType.SIMPLE_STRING;
                break;
            case '-':
                type = DecodeType.ERROR_STRING;
                break;
            case '$':
                type = DecodeType.RAW_STRING;
                break;
            default:
                type = DecodeType.INLINE_CMD;
        }
        checkType();
        state = DecodeState.DECODE_INLINE;
    }

    public void decode(ByteBuf buf) {
        while (buf.isReadable()) {
            switch (state) {
                case DECODE_TYPE:
                    decodeType(buf.readByte());
                    break;
                case DECODE_INLINE:

            }
        }
    }

    protected void checkType() {

    }

    void decodeInline() {

    }

}
