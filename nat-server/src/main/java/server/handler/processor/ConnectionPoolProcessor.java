package server.handler.processor;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class ConnectionPoolProcessor implements Processor {

    @Override
    public void process(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        //客户端对建立连接池命令的响应，无业务需要暂时不实现
    }
}
