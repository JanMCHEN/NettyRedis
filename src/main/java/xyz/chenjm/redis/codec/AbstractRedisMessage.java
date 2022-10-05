package xyz.chenjm.redis.codec;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;

abstract public class AbstractRedisMessage implements RedisMessage1 {
    protected ByteBuf buf;
    @Override
    public ByteBuf getBuf() {
        return buf;
    }

    @Override
    public byte[] getBytes() {
        int sz = buf.readableBytes();
        byte[] dst = new byte[sz];
        buf.getBytes(buf.readerIndex(), dst);
        return dst;
    }

    @Override
    public int refCnt() {
        return buf.refCnt();
    }

    @Override
    public ReferenceCounted retain() {
        return buf.retain();
    }

    @Override
    public ReferenceCounted retain(int increment) {
        return buf.retain(increment);
    }

    @Override
    public ReferenceCounted touch() {
        return buf.touch();
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        return buf.touch(hint);
    }

    @Override
    public boolean release() {
        return buf.release();
    }

    @Override
    public boolean release(int decrement) {
        return buf.release(decrement);
    }
}
