package core.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author wneck130@gmail.com
 * @Description: 协议分发控制器，根据收到的首包消息内容进行判断，区分TCP和HTTP协议
 * @date 2021/9/26
 */
@Slf4j
public abstract class DispatcherHandler extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        // Will use the first five bytes to detect a protocol.
        if (byteBuf.readableBytes() < 5) {
            return;
        }
        final int magic1 = byteBuf.getUnsignedByte(byteBuf.readerIndex());
        final int magic2 = byteBuf.getUnsignedByte(byteBuf.readerIndex() + 1);

        // 判断是不是HTTP请求
        if (isHttp(magic1, magic2)) {
            log.info("this is a http msg");
            addHttpHandler(channelHandlerContext);
        } else {
            log.info("this is a socket msg");
//            addTcpHandler(channelHandlerContext);
        }
        channelHandlerContext.pipeline().remove(this);
    }

    /**
     * 配置http请求的pipeline
     * @param ctx
     */
    public abstract void addHttpHandler(ChannelHandlerContext ctx);

    /**
     * 配置Tcp请求的pipeline
     * @param ctx
     */
    public abstract void addTcpHandler(ChannelHandlerContext ctx);

    /**
     * 判断请求是否是HTTP请求
     *
     * @param magic1 报文第一个字节
     * @param magic2 报文第二个字节
     * @return
     */
    public boolean isHttp(int magic1, int magic2) {
        // GET
        return magic1 == 'G' && magic2 == 'E' ||
                // POST
                magic1 == 'P' && magic2 == 'O' ||
                // PUT
                magic1 == 'P' && magic2 == 'U' ||
                // HEAD
                magic1 == 'H' && magic2 == 'E' ||
                // OPTIONS
                magic1 == 'O' && magic2 == 'P' ||
                // PATCH
                magic1 == 'P' && magic2 == 'A' ||
                // DELETE
                magic1 == 'D' && magic2 == 'E' ||
                // TRACE
                magic1 == 'T' && magic2 == 'R' ||
                // CONNECT
                magic1 == 'C' && magic2 == 'O';
    }
}
