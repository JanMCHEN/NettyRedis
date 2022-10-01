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


    RedisMessageDecoder<?extends RedisMessage> onDecoder;
    List<RedisMessageDecoder<? extends RedisMessage>> candidateDecoders;

    List<RedisMessage> messageList;

    private DecodeState state;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state) {
            case DECODE_TYPE:

        }
    }

    private void decodeType(ByteBuf in) {

    }

    protected RedisMessageDecoder<?extends RedisMessage> getDecoder(ByteBuf in) {
        if (onDecoder == null) {

        }
        return onDecoder;
    }

    static final class CommandArrayDecoder implements RedisMessageDecoder<RedisMessage> {
        FullBulkStringMessageDecoder stringDecoder;
        RedisMessageDecoder<? extends RedisMessage> curr;

        boolean inline = true;

        RedisMessage result;

        @Override
        public RedisMessage decode(ByteBuf in) {
            if (curr == null) {
                if (in.getByte(in.readerIndex()) == RedisMessage.ARRAY_TYPE) {
                    curr = new ArrayHeaderMessageDecoder();
                } else if (inline) {
                    curr = new InlineCommandMessageDecoder();
                }else {
                    throw new ProtocolException();
                }
            }
            RedisMessage decode = curr.decode(in);
            if (decode == null) {
                return null;
            }
            if (decode instanceof RedisMessage.ArrayHeaderMessage) {

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
            return in.getByte(in.readerIndex())==RedisMessage.ARRAY_TYPE;
        }
    }
}
