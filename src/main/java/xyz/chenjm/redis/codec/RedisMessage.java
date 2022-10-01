package xyz.chenjm.redis.codec;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;

import java.nio.charset.StandardCharsets;

public interface RedisMessage extends ReferenceCounted {
    short CR_LF = ('\r' << 8 & 0xff) | '\n';
    byte SIMPLE_TYPE = '+';
    byte STRING_TYPE = '$';
    byte ARRAY_TYPE = '*';
    byte ERROR_TYPE = '-';
    byte INT_TYPE = ':';

    static void checkLineEnd(ByteBuf buf, int off) {
        if(buf.getShort(off)==CR_LF)
            return;
        throw new IllegalArgumentException();
    }

    static void checkInteger(ByteBuf buf, int off, int length) {
        if (length == 0 && buf.getByte(off)==0) {
            checkLineEnd(buf, off+1);
        } else if (length == -1 && buf.getByte(off)=='-' && buf.getByte(off+1)=='1') {
            checkLineEnd(buf, off+2);
        } else if (length > 0) {
            int t = 0;
            while (t < length) {
                int i = buf.getByte(off++) - '0';
                if (i <0 || i>9)
                    break;
                t = t * 10 + i;
            }
            if (t == length) {
                checkLineEnd(buf, off);
            }
        }
    }

    ByteBuf getBuf();

    byte[] getBytes();

    abstract class InlineMessage extends AbstractRedisMessage {
        @Override
        public String toString() {
            return buf.toString(buf.readerIndex()+1, buf.readableBytes()-3, StandardCharsets.UTF_8);
        }

        public InlineMessage(ByteBuf buf, byte first) {
            this(buf, first, false);
        }

        public InlineMessage(ByteBuf buf, boolean unsafe) {
            this.buf = buf;
            if (!unsafe) {
                if (buf.getShort(buf.writerIndex()-2)!=CR_LF) {
                    throw new RuntimeException();
                }
            }
        }

        InlineMessage(ByteBuf buf, byte first, boolean unsafe) {
            this.buf = buf;
            if (!unsafe) {
                if (buf.getByte(buf.readerIndex())!=first || buf.getShort(buf.writerIndex()-2)!=CR_LF) {
                    throw new RuntimeException();
                }
            }
        }
    }
    final class SimpleMessage extends InlineMessage {

        public SimpleMessage(ByteBuf buf) {
            super(buf, SIMPLE_TYPE, false);
        }
        SimpleMessage(ByteBuf buf, boolean unsafe) {
            super(buf, SIMPLE_TYPE, unsafe);
        }

    }
    class ErrorMessage extends InlineMessage {

        public ErrorMessage(ByteBuf buf) {
            super(buf, ERROR_TYPE, false);
        }
        ErrorMessage(ByteBuf buf, boolean unsafe) {
            super(buf, ERROR_TYPE, unsafe);
        }

    }
    final class IntegerMessage extends InlineMessage {
        private int num;

        public IntegerMessage(ByteBuf buf) {
            super(buf, INT_TYPE, false);
        }

        IntegerMessage(ByteBuf buf, boolean unsafe) {
            super(buf, INT_TYPE, unsafe);
            num = Integer.parseInt(toString());
        }
        public int getNum() {
            return num;
        }

    }

    final class InlineCommandMessage extends InlineMessage {

        public InlineCommandMessage(ByteBuf buf) {
            super(buf, false);
        }
        InlineCommandMessage(ByteBuf buf, boolean unsafe) {
            super(buf, unsafe);
        }

        @Override
        public String toString() {
            return buf.toString(buf.readerIndex(), buf.readableBytes()-2, StandardCharsets.UTF_8);
        }
    }

    class ArrayHeaderMessage extends InlineMessage {

        public ArrayHeaderMessage(ByteBuf buf) {
            super(buf, ARRAY_TYPE, false);
        }

        ArrayHeaderMessage(ByteBuf buf, boolean unsafe) {
            super(buf, ARRAY_TYPE, unsafe);
        }
    }

    abstract class CollectionMessage extends AbstractRedisMessage{}
}
