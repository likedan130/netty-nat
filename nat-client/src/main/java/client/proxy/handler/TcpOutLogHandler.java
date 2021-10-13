package client.proxy.handler;

import core.utils.BufUtil;
import core.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wneck130@gmail.com
 * @Description:
 * @date 2021/9/27
 */
@Slf4j
public class TcpOutLogHandler extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        log.debug("ProxyClient发送消息：{}", ByteUtil.toHexString(BufUtil.getArray((ByteBuf) msg)));
        super.write(ctx, msg, promise);
    }
}
