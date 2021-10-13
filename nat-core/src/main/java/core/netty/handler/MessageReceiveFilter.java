package core.netty.handler;

import core.constant.FrameConstant;
import core.entity.Frame;
import core.netty.group.channel.message.MessageContext;
import core.netty.group.channel.message.ResponseEvent;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wneck130@gmail.com
 * @Description: 消息响应过滤器，推送消息响应事件
 * @date 2021/9/28
 */
@Slf4j
public class MessageReceiveFilter extends SimpleChannelInboundHandler<Frame> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Frame msg) throws Exception {
        try {
            //服务端主动发送的消息才需要监听响应
            if (msg.getPv() == FrameConstant.RES_PV) {
                //生成事件源
                MessageContext messageContext = MessageContext.getInstance();
                //生成对应的responseEvent
                String channelId = msg.getReq();
                ResponseEvent responseEvent = new ResponseEvent(messageContext);
                responseEvent.setChannelId(channelId);
                responseEvent.setRequest(MessageContext.getHistoryFrame(channelId, msg.getSerial()));
                responseEvent.setResponse(msg);
                messageContext.notifyResponse(responseEvent);
                return;
            }
            ctx.fireChannelRead(msg);
        } catch (Exception e) {
            log.error("{}\n生成响应消息事件失败", msg.toString(), e);
        }
    }
}
