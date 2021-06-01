package server.handler.processor;

import core.constant.FrameConstant;
import core.entity.Frame;
import core.processor.Processor;
import core.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import server.group.ServerChannelGroup;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * @Author wneck130@gmail.com
 * @function 数据交互处理器，转发来自internalClient的业务消息
 */
@Slf4j
public class DataTransferProcessor implements Processor {

    @Override
    public void process(ChannelHandlerContext ctx, Frame msg) throws Exception {
        Channel internalChannel = ctx.channel();
        if (ServerChannelGroup.channelPairExist(internalChannel.id())) {
            //根据internalChannel找到对应的proxyChannel
            Channel proxyChannel = ServerChannelGroup.getProxyByInternal(internalChannel.id());
            if (proxyChannel == null) {
                log.error("配对关系异常：[InternalChannel：{}, ProxyChannel：null]", internalChannel);
                throw new Exception("配对关系异常：[InternalChannel："+internalChannel+", ProxyChannel：null]");
            }
            ServerChannelGroup.cancelFuture(proxyChannel.id());
            log.debug("Responsor数据：" + msg.toString() +
                            "\n Requestor--[{}]--ServerProxy----ServerInternal<<[{}]<<Client",
                    proxyChannel.id(), internalChannel.id());
            ByteBuf response = Unpooled.buffer();
            response.writeBytes((byte[])msg.getData().get("data"));
            proxyChannel.writeAndFlush(response).addListener((future -> {
                if (future.isSuccess()) {
                    log.debug("Responsor数据：" + msg.toString() +
                                    "\n Requestor<<[{}]<<ServerProxy----ServerInternal--[{}]--Client",
                            proxyChannel.id(), internalChannel.id());
                }
            }));
        } else {
            //proxy连接可能已经被主动断开，忽略本条消息
            log.debug("Responsor数据发送失败：" + msg.toString() +
                            "\n Requestor--XX--ServerProxy----ServerInternal--[{}]--Client",
                    internalChannel.id());
            return;
        }
    }
}
