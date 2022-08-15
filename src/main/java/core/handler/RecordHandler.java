package core.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;

public class RecordHandler extends SimpleChannelInboundHandler<ByteBuf> {
    CompositeByteBuf record = Unpooled.compositeBuffer();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
        record.addComponent(true, buf);
        // 添加一个引用，后续需要再次读取
        record.retain();
        ctx.fireChannelRead(record);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().attr(AttributeKey.valueOf("cmd")).set(record);
        super.channelActive(ctx);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        record = Unpooled.compositeBuffer();
        super.channelReadComplete(ctx);
    }
}
