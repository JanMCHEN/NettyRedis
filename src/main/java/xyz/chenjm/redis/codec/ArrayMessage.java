package xyz.chenjm.redis.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCounted;

public class ArrayMessage implements RedisMessage1 {
    ArrayHeaderMessage header;
    RedisMessage1[] child;

    volatile ByteBuf buf;

    @Override
    public ByteBuf getBuf() {
        if (buf == null) {
            synchronized (this) {
                if (buf==null) {
                    ByteBuf[] arr = new ByteBuf[child.length+1];
                    arr[0] = header.buf;
                    for (int i=1;i<arr.length;++i) {
                        arr[i] = child[i].getBuf();
                    }
                    buf = Unpooled.wrappedUnmodifiableBuffer(arr);
                }
            }
        }
        return buf;
    }

    @Override
    public byte[] getBytes() {
        return null;
    }

    @Override
    public int refCnt() {
        return header.refCnt();
    }

    @Override
    public ReferenceCounted retain() {
        return null;
    }

    @Override
    public ReferenceCounted retain(int increment) {
        return null;
    }

    @Override
    public ReferenceCounted touch() {
        return null;
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        return null;
    }

    @Override
    public boolean release() {
        return false;
    }

    @Override
    public boolean release(int decrement) {
        return false;
    }
}
