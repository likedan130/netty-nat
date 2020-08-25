package server.handler;

import core.constant.FrameConstant;
import core.utils.BufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpHeaderNames;

public class HttpHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        String httpMsg = "";
        try{
            String msgStr = new String(BufUtil.getArray(msg), FrameConstant.DEFAULT_CHARSET);
            if (msgStr.contains("HTTP") || msgStr.contains("http")) {
                if (msgStr.contains(HttpHeaderNames.HOST)) {
                    httpMsg = msgStr.substring(0, msgStr.indexOf("Host")) + msgStr.substring(msgStr.indexOf("Host")).substring(msgStr.substring(msgStr.indexOf("Host")).indexOf("\r\n") + 2);
                    ByteBuf newMsg = Unpooled.buffer().writeBytes(httpMsg.getBytes(FrameConstant.DEFAULT_CHARSET));
                    ctx.fireChannelRead(newMsg);
                    return;
                }
            }
            msg.retain();
            ctx.fireChannelRead(msg);
        } catch (Exception e) {
            e.printStackTrace();
            ctx.fireChannelRead(msg);
        }
    }
}
