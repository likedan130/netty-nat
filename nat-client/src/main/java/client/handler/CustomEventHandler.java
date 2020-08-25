package client.handler;

import client.group.ClientChannelGroup;
import core.entity.Frame;
import core.enums.CommandEnum;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;

public class CustomEventHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        //channel触发空闲时，发送心跳，确保连接正常
        if (evt instanceof IdleStateEvent) {
        }
     }
}
