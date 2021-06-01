package server.handler;

import core.constant.FrameConstant;
import core.utils.BufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

/**
 * @author wneck130@gmail.com
 * @function 简易的http报文内容替换，将http请求头中的host信息去除
 */
public class HttpHandler extends ChannelOutboundHandlerAdapter {

    private static final String HTTP_HOST = "Host";

    private static final String HTTP_CONTENT_LENGTH = "Content-Length";

    private static final String HTTP_METHOD = "GET";

    /**
     * 如果采用http1.0协议发送get请求，请求头中的Content-Length值与实际http包大小不一致，
     * 会导致异常，Content-Length值偏小则超时，Content-Length值偏大则包内容补全
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        String msgStr = new String(BufUtil.getArray((ByteBuf) msg), FrameConstant.DEFAULT_CHARSET);
        if (msgStr.contains(HTTP_CONTENT_LENGTH)) {
            String httpMsg = msgStr.substring(0,
                    msgStr.indexOf(HTTP_CONTENT_LENGTH))
                    + msgStr.substring(msgStr.indexOf(HTTP_CONTENT_LENGTH))
                    .substring(msgStr.substring(msgStr.indexOf(HTTP_CONTENT_LENGTH)).indexOf("\r\n")
                            + 2);
            ByteBuf newMsg = Unpooled.buffer().writeBytes(httpMsg.getBytes(FrameConstant.DEFAULT_CHARSET));
            ctx.writeAndFlush(newMsg);
        }
        ctx.writeAndFlush(msg);
    }

}
