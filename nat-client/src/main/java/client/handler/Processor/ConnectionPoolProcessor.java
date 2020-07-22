package client.handler.Processor;

import client.InternalClient;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class ConnectionPoolProcessor implements Processor {

    @Override
    public void process(ChannelHandlerContext ctx, ByteBuf msg) {
        int connectionNum = msg.getByte(12) & 0xFF;
        //根据服务器要求，启动对应数量客户端连接
        InternalClient internalClient = new InternalClient();
        try {
            internalClient.init();
            //创建子线程启动内部连接池
            new Thread(()->{
                try {
                    //TODO 测试时固定建立一条连接
//                    internalClient.start(connectionNum);
                    internalClient.start(100);
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
