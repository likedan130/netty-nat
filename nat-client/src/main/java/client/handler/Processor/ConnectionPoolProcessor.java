package client.handler.Processor;

import client.InternalClient;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.util.Objects;

public class ConnectionPoolProcessor implements Processor {

    @Override
    public void process(ChannelHandlerContext ctx, ByteBuf msg) {
        //可读长度不够，说明不符合协议，不做解析
        if (msg.readableBytes() < 12) {
            return;
        }
        int connectionNum = msg.getByte(12) & 0xFF;
        //根据服务器要求，启动对应数量客户端连接
        InternalClient internalClient = new InternalClient();
        try {
            internalClient.init();
            internalClient.start(connectionNum);
        } catch (Exception e) {
            e.printStackTrace();
            ctx.close();
        }

    }
}
