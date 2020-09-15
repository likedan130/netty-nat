package server.handler;

import core.constant.FrameConstant;
import core.utils.BufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;

/**
 * @function 简易的http报文内容替换，将http请求头中的host信息去除
 * @author wneck130@gmail.com
 */
public class HttpHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final String HTTP_HOST = "Host";

    /**
     * 限于现有的转发模式，需要将http请求头中host信息删除才能正确访问
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        String msgStr = new String(BufUtil.getArray(msg), FrameConstant.DEFAULT_CHARSET);
        if (msgStr.contains(HTTP_HOST)) {
            String httpMsg = msgStr.substring(0,
                    msgStr.indexOf(HTTP_HOST))
                    + msgStr.substring(msgStr.indexOf(HTTP_HOST))
                        .substring(msgStr.substring(msgStr.indexOf(HTTP_HOST)).indexOf("\r\n")
                    + 2);
            ByteBuf newMsg = Unpooled.buffer().writeBytes(httpMsg.getBytes(FrameConstant.DEFAULT_CHARSET));
            ctx.fireChannelRead(newMsg);
            return;
        }
    }
}
