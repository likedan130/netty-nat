package server.handler;

import core.constant.FrameConstant;
import core.utils.BufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpMethod;

/**
 * @author wneck130@gmail.com
 * @function handler转化器，根据首次请求的内容重新设置pipeline中的handler
 */
public class ConverterHandler extends SimpleChannelInboundHandler<ByteBuf> {


    /**
     * 通过报文内容判断是否为http/https请求
     * 如果是https的connect请求，则直接返回固定报文给客户端，后续所有加密通信内容不做干涉
     * 如果是http/https请求，将请求头中的host信息去除
     * 如果是tcp自定义报文，则将本handler和httpHandler移除
     * HTTP请求由请求行、请求头、请求体构成，每个部分由换行符分隔，同一部分内部使用空格分隔，请求行内容为请求方法，URI，协议版本
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        try {
            String msgStr = new String(BufUtil.getArray(msg), FrameConstant.DEFAULT_CHARSET);
            String headeLine = msgStr.split("\\n")[0];
            //判断是否为https的connect请求，处理浏览器的代理连接请求
            if (headeLine.substring(0, headeLine.indexOf(" ") + 1).equalsIgnoreCase(HttpMethod.CONNECT.name())) {
                ByteBuf buffer = ctx.channel().alloc().buffer("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes().length);
                buffer.writeBytes("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes());
                ctx.channel().writeAndFlush(buffer);
                return;
            } else {
                //普通http请求直接调整handler，转发给下一个handler
                ctx.pipeline().remove("converter");
                msg.retain();
                ctx.fireChannelRead(msg);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //如果无法处理为http请求，则移除对应http相关handler，并将消息转发至下一个handler
        ctx.pipeline().remove("converter");
        ctx.pipeline().remove("http");
        msg.retain();
        ctx.fireChannelRead(msg);
    }
}
