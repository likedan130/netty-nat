package client.handler;

import core.entity.Frame;
import core.enums.CommandEnum;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * @Author wneck130@gmail.com
 * @function
 */
public class CustomEventHandler extends ChannelInboundHandlerAdapter {
    /**
     * 事件触发处理方法，向pipeline注册相关事件的监听，触发后使用本方法进行处理
     *
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        //channel触发空闲时，发送心跳，确保连接正常
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.ALL_IDLE) {
                Frame frame = new Frame();
                frame.setCmd(CommandEnum.CMD_HEARTBEAT.getCmd());
                ctx.writeAndFlush(frame);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
