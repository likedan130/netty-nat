package client.handler.Processor;

import client.group.ClientChannelGroup;
import core.entity.Frame;
import core.processor.Processor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author wneck130@gmail.com
 * @function 数据交互处理器，转发来自internalClient的业务消息
 */
@Slf4j
public class DataTransferProcessor implements Processor {

    @Override
    public void process(ChannelHandlerContext ctx, Frame msg) throws Exception {

        //根据internalChannel找到对应的proxyChannel
        if (ClientChannelGroup.channelPairExist(ctx.channel().id())) {
            ByteBuf response = Unpooled.buffer();
            response.writeBytes((byte[])msg.getData().get("data"));
            Channel proxyChannel = ClientChannelGroup.getProxyByInternal(ctx.channel().id());
            proxyChannel.writeAndFlush(response).addListener((future -> {
                if (future.isSuccess()) {
                    log.debug("向{}发送业务数据{}成功!!!", proxyChannel.id(), (byte[])msg.getData().get("data"));
                }
            }));
        } else {
            //proxy连接可能已经被主动断开，忽略本条消息
            return;
        }
    }
}
