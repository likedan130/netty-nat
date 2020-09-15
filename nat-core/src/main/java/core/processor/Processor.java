package core.processor;

import core.entity.Frame;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * @Author wneck130@gmail.com
 * @function
 */
public interface Processor {

    /**
     * Frame消息处理器接口
     * @param ctx
     * @param msg
     * @throws Exception
     */
    void process(ChannelHandlerContext ctx, Frame msg) throws Exception;
}
