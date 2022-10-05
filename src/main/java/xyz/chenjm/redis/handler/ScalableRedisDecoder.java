package xyz.chenjm.redis.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import xyz.chenjm.redis.codec.*;
import xyz.chenjm.redis.exception.ProtocolException;

import java.util.List;

public class ScalableRedisDecoder extends ByteToMessageDecoder {
    enum DecodeState {
        DECODE_TYPE, DECODE_NUMBER, DECODE_INLINE
    }


    RedisMessageDecoder<?extends RedisMessage1> onDecoder;
    List<RedisMessageDecoder<? extends RedisMessage1>> candidateDecoders;

    List<RedisMessage1> messageList;

    private DecodeState state;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state) {
            case DECODE_TYPE:

        }
    }

    private void decodeType(ByteBuf in) {

    }

    protected RedisMessageDecoder<?extends RedisMessage1> getDecoder(ByteBuf in) {
        if (onDecoder == null) {

        }
        return onDecoder;
    }

    static final class CommandArrayDecoder implements RedisMessageDecoder<RedisMessage1> {
        FullBulkStringMessageDecoder stringDecoder;
        RedisMessageDecoder<? extends RedisMessage1> curr;

        boolean inline = true;

        RedisMessage1 result;

        @Override
        public RedisMessage1 decode(ByteBuf in) {
            if (curr == null) {
                if (in.getByte(in.readerIndex()) == RedisMessage1.ARRAY_TYPE) {
                    curr = new ArrayHeaderMessageDecoder();
                } else if (inline) {
                    curr = new InlineCommandMessageDecoder();
                }else {
                    throw new ProtocolException();
                }
            }
            RedisMessage1 decode = curr.decode(in);
            if (decode == null) {
                return null;
            }
            if (decode instanceof RedisMessage1.ArrayHeaderMessage) {

            }
            if (curr instanceof InlineCommandMessageDecoder) {
                return decode;
            }
            return null;
        }

        @Override
        public void reset() {

        }

        @Override
        public boolean support(ByteBuf in) {
            return in.getByte(in.readerIndex())== RedisMessage1.ARRAY_TYPE;
        }
    }
}
