package core.processor;

import core.entity.Frame;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public interface Processor {

    void process(ChannelHandlerContext ctx, Frame msg) throws Exception;
}
