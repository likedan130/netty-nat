package client.handler.Processor;

import client.InternalClient;
import client.ProxyClient;
import client.group.ClientChannelGroup;
import core.constant.NumberConstant;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;

public class ConnectionPoolProcessor implements Processor {

    @Override
    public void process(ChannelHandlerContext ctx, ByteBuf msg) {
        int connectionNum = msg.getByte(NumberConstant.TWELVE) & 0xFF;
        //根据服务器要求，启动对应数量客户端连接
        InternalClient internalClient = new InternalClient();
        ProxyClient proxyClient = new ProxyClient();
        try {
            internalClient.init();
            //启动内部连接池
            internalClient.start(connectionNum);
            //启动代理服务
            proxyClient.init();
            List<Object> list = proxyClient.start();
            ClientChannelGroup.proxyClient.addAll(list);
        } catch (Exception e) {
            e.printStackTrace();
            ctx.close();
        }
    }
}
