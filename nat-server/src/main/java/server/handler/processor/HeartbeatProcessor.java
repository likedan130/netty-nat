package server.handler.processor;

import core.entity.Frame;
import core.processor.Processor;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author wneck130@gmail.com
 * @Function 心跳处理器
 */
@Slf4j
public class HeartbeatProcessor implements Processor {
    @Override
    public void process(ChannelHandlerContext ctx, Frame msg) throws Exception {
        //保持连接活性，无需回复内容
    }
}
