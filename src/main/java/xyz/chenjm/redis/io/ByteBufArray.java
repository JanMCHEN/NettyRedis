package xyz.chenjm.redis.io;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;

import java.util.Arrays;
import java.util.Objects;

public class ByteBufArray implements ByteBufHolder {
    ByteBuf buf;
    int[] offsetArray;

    public ByteBufArray(ByteBuf buf, int[] off) {
        this.buf = buf;
        this.offsetArray = off;
    }

    public ByteBuf get(int i) {
        Objects.checkIndex(i, offsetArray.length-1);
        return buf.slice(offsetArray[i], offsetArray[i+1]-offsetArray[i]);
    }

    @Override
    public ByteBuf content() {
        return buf;
    }

    @Override
    public ByteBufHolder copy() {
        return new ByteBufArray(buf.copy(), Arrays.copyOf(offsetArray, offsetArray.length));
    }

    @Override
    public ByteBufHolder duplicate() {
        return new ByteBufArray(buf.duplicate(), Arrays.copyOf(offsetArray, offsetArray.length));
    }

    @Override
    public ByteBufHolder retainedDuplicate() {
        return new ByteBufArray(buf.retainedDuplicate(), Arrays.copyOf(offsetArray, offsetArray.length));
    }

    @Override
    public ByteBufHolder replace(ByteBuf content) {
        throw new  UnsupportedOperationException();
    }

    @Override
    public int refCnt() {
        return buf.refCnt();
    }

    @Override
    public ByteBufHolder retain() {
        buf.retain();
        return this;
    }

    @Override
    public ByteBufHolder retain(int increment) {
        buf.retain(increment);
        return this;
    }

    @Override
    public ByteBufHolder touch() {
        return this;
    }

    @Override
    public ByteBufHolder touch(Object hint) {
        return this;
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
