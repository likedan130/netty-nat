package server.handler;

import core.entity.Frame;
import core.enums.CommandEnum;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * @Author wneck130@gmail.com
 * @Function 用户自定义事件处理器
 */
public class CustomEventHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        //监测到连接空闲，则断开连接
        if (evt instanceof IdleStateEvent) {
            Frame frame = new Frame();
            frame.setCmd(CommandEnum.CMD_HEARTBEAT.getCmd());
            ctx.writeAndFlush(frame);
        }
    }
}
