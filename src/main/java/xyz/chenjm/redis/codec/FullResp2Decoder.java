package xyz.chenjm.redis.codec;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

public class FullResp2Decoder implements RedisMessageDecoder<RedisMessage>{
    List<RedisMessageDecoder<? extends RedisMessage>> decoders = new ArrayList<>();
    private InlineProcessor lineEndProcessor = new InlineProcessor();
    private IntegerInlineProcessor intEndProcessor = new IntegerInlineProcessor();

    private SimpleMessageDecoder simpleDecoder = new SimpleMessageDecoder(lineEndProcessor);
    private ArrayHeaderMessageDecoder arrayHeaderDecoder = new ArrayHeaderMessageDecoder(intEndProcessor);


    RedisMessageDecoder<?extends RedisMessage> doDecoder;

    @Override
    public RedisMessage decode(ByteBuf in) {
        if (doDecoder == null) {

        }
        return null;
    }

    @Override
    public void reset() {

    }

    @Override
    public boolean support(ByteBuf in) {
        byte flag = in.getByte(in.readerIndex());
        switch (flag) {

        }
        return false;
    }
}
