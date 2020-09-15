package server.handler.processor;

import core.entity.Frame;
import core.processor.Processor;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * @Author wneck130@gmail.com
 * @Function
 */
public class ConnectionReduceProcessor implements Processor {

    @Override
    public void process(ChannelHandlerContext ctx, Frame msg) throws Exception {

    }
}
