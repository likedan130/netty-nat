package client.handler.processor;

import core.entity.Frame;
import core.processor.Processor;
import io.netty.channel.ChannelHandlerContext;

/**
 * @Author wneck130@gmail.com
 * @function
 */
public class HeartbeatProcessor implements Processor {

    @Override
    public void process(ChannelHandlerContext ctx, Frame msg) throws Exception {
        //TODO 预留心跳命令，对于长连接的保持有特殊需要时启用心跳
    }
}
