package client.handler.processor;

import client.ProxyClient;
import client.group.ClientChannelGroup;
import core.entity.Frame;
import core.processor.Processor;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author wneck130@gmail.com
 * @function
 */
@Slf4j
public class ConnectToProxyProcessor implements Processor {

    @Override
    public void process(ChannelHandlerContext ctx, Frame msg) {
        //收到服务器的命令后主动建立与被代理服务之间的连接
        ProxyClient proxyClient = new ProxyClient();
        log.debug("internalChannel:{} 收到服务器建立代理连接指令!!!", ctx.channel().id());
        try {
            //启动代理服务
            proxyClient.init();
            Channel internalChannel = ctx.channel();
            ChannelFuture future = proxyClient.start();
            future.get();
            if (future.isSuccess()) {
                ClientChannelGroup.addChannelPair(internalChannel, future.channel());
                log.debug("建立配对："+"["+internalChannel.id()+", "+future.channel().id()+"]");
                ClientChannelGroup.removeIdleInternalChannel(internalChannel);
                ClientChannelGroup.addInternalChannel(internalChannel);
            }
        } catch (Exception e) {
            e.printStackTrace();
            ctx.close();
        }
    }
}
