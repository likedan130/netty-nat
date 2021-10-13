package core.netty.handler;

import core.constant.FrameConstant;
import core.entity.Frame;
import core.netty.group.channel.message.MessageContext;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

/**
 * @author wneck130@gmail.com
 * @Description: 消息请求过滤器
 * 每一条发送的消息存储至 {@link core.netty.group.channel.message.MessageContext}的历史消息中等待响应
 * @date 2021/9/30
 */
public class MessageSendFilter extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        Frame frame = (Frame) msg;
        if (frame.getPv() == FrameConstant.REQ_PV) {
            MessageContext.addHistoryFrame(frame.getReq(), frame);
        }
        super.write(ctx, msg, promise);
    }
}
