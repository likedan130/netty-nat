package server.handler.processor;

import core.entity.Frame;
import core.processor.Processor;
import io.netty.channel.ChannelHandlerContext;

/**
 * @Author wneck130@gmail.com
 * @Function
 */
public class ConnectionExpandProcessor implements Processor {

    @Override
    public void process(ChannelHandlerContext ctx, Frame msg) throws Exception {
        //客户端对建立连接池命令的响应，无业务需要暂时不实现
    }
}
