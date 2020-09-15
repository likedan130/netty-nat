package client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * @Author wneck130@gmail.com
 * @function
 */
public class CustomEventHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        //channel触发空闲时，发送心跳，确保连接正常
        if (evt instanceof IdleStateEvent) {
        }
     }
}
