package client.handler.Processor;

import client.InternalClient;
import core.constant.NumberConstant;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class ConnectionPoolProcessor implements Processor {

    @Override
    public void process(ChannelHandlerContext ctx, ByteBuf msg) {
        int connectionNum = msg.getByte(NumberConstant.TWELVE) & 0xFF;
        //根据服务器要求，启动对应数量客户端连接
        InternalClient internalClient = new InternalClient();
        try {
            internalClient.init();
            //创建子线程启动内部连接池
            new Thread(()->{
                try {
                   internalClient.start(connectionNum);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
            ctx.close();
        }

    }
}
