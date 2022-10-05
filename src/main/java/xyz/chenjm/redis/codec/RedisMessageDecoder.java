package xyz.chenjm.redis.codec;

import io.netty.buffer.ByteBuf;
import io.netty.util.ByteProcessor;

public interface RedisMessageDecoder<T extends RedisMessage1> extends ByteBufDecoder<T>{
    T decode(ByteBuf in);

    void reset();

    boolean support(ByteBuf in);

    abstract class InlineMessageDecoder<T extends RedisMessage1.InlineMessage> implements RedisMessageDecoder<T> {
        int offset = -1;
        InlineProcessor inlineProcessor;

        abstract protected T newMessage(ByteBuf buf);

        @Override
        public T decode(ByteBuf in) {
            if (offset == -1) {
                offset = in.readerIndex();
                inlineProcessor.reset();
            }
            int idx = in.forEachByte(offset, in.writerIndex()-offset, inlineProcessor);
            if (idx == -1) {
                offset = in.writerIndex();
                return null;
            }
            offset = idx + 1;
            int reader = in.readerIndex();
            in.readerIndex(offset);
            ByteBuf slice = in.slice(reader, offset - reader);
            in.readerIndex(offset);
            return newMessage(slice);
        }

        @Override
        public void reset() {
            offset = -1;
            inlineProcessor.reset();
        }
    }

    class SimpleMessageDecoder extends InlineMessageDecoder<RedisMessage1.SimpleMessage> {
        public SimpleMessageDecoder(InlineProcessor p) {
            inlineProcessor = p;
        }
        public SimpleMessageDecoder() {
            inlineProcessor = new InlineProcessor();
        }
        @Override
        protected RedisMessage1.SimpleMessage newMessage(ByteBuf buf) {
            return new RedisMessage1.SimpleMessage(buf, true);
        }

        @Override
        public boolean support(ByteBuf in) {
            return in.getByte(in.readerIndex()) == RedisMessage1.SIMPLE_TYPE;
        }
    }

    class InlineCommandMessageDecoder extends InlineMessageDecoder<RedisMessage1.InlineCommandMessage> {
        public InlineCommandMessageDecoder(InlineProcessor p) {
            inlineProcessor = p;
        }
        public InlineCommandMessageDecoder() {
            inlineProcessor = new InlineProcessor();
        }
        @Override
        public boolean support(ByteBuf in) {
            return true;
        }

        @Override
        protected RedisMessage1.InlineCommandMessage newMessage(ByteBuf buf) {
            return new RedisMessage1.InlineCommandMessage(buf, true);
        }
    }

    class ArrayHeaderMessageDecoder extends InlineMessageDecoder<RedisMessage1.ArrayHeaderMessage> {
        public ArrayHeaderMessageDecoder() {
            inlineProcessor = new IntegerInlineProcessor();
        }

        public ArrayHeaderMessageDecoder(IntegerInlineProcessor p) {
            inlineProcessor = p;
        }

        @Override
        protected RedisMessage1.ArrayHeaderMessage newMessage(ByteBuf buf) {
            return new RedisMessage1.ArrayHeaderMessage(buf, true);
        }

        @Override
        public boolean support(ByteBuf in) {
            return in.getByte(in.readerIndex()) == RedisMessage1.SIMPLE_TYPE;
        }
    }

    class InlineProcessor implements ByteProcessor {
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

    class IntegerInlineProcessor extends InlineProcessor {
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
}
