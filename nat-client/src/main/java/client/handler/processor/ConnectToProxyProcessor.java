package client.handler.processor;

import client.group.ClientChannelGroup;
import core.entity.Frame;
import core.processor.Processor;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author wneck130@gmail.com
 * @function
 */
@Slf4j
public class ConnectToProxyProcessor implements Processor {

    @Override
    public void process(ChannelHandlerContext ctx, Frame msg) throws Exception {
        try {
            //收到服务器的命令后主动建立与被代理服务之间的连接
            ClientChannelGroup.connectToProxy(ctx);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
