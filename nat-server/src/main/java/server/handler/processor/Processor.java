package server.handler.processor;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public interface Processor {

    void process(ChannelHandlerContext ctx, ByteBuf msg) throws Exception;
}
