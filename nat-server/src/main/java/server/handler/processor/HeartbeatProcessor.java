package server.handler.processor;

import core.entity.Frame;
import core.processor.Processor;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author wneck130@gmail.com
 * @Function 心跳处理器
 */
public class HeartbeatProcessor implements Processor {
    Logger log = LoggerFactory.getLogger(HeartbeatProcessor.class);
    @Override
    public void process(ChannelHandlerContext ctx, Frame msg) throws Exception {
        //TODO 预留心跳命令，对于长连接的保持有特殊需要时启用心跳
    }
}
