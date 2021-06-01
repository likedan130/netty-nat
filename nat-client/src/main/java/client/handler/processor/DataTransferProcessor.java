package client.handler.processor;

import client.ProxyClient;
import client.group.ClientChannelGroup;
import core.entity.Frame;
import core.processor.Processor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author wneck130@gmail.com
 * @function 数据交互处理器，转发来自internalClient的业务消息
 */
@Slf4j
public class DataTransferProcessor implements Processor {

    @Override
    public void process(ChannelHandlerContext ctx, Frame msg) throws Exception {
        Channel internalChannel = ctx.channel();
        //根据internalChannel找到对应的proxyChannel
        if (ClientChannelGroup.channelPairExist(internalChannel.id())) {
            Channel proxyChannel = ClientChannelGroup.getProxyByInternal(internalChannel.id());
            if (proxyChannel == null) {
                log.error("配对关系异常：[InternalChannel：{}, ProxyChannel：null]", internalChannel);
//                throw new Exception("配对关系异常：[InternalChannel："+internalChannel+", ProxyChannel：null]");
                proxyChannel = ClientChannelGroup.connectToProxy(ctx).channel();
                log.debug("Requestor数据：" + msg.toString() +
                                "\n Server>>[{}]>>ClientInternal--[{}]--ClientProxy--[{}]--Responsor"
                        , internalChannel.id(), proxyChannel.id());
                ByteBuf response = Unpooled.buffer();
                response.writeBytes((byte[]) msg.getData().get("data"));
                final ChannelId channelId = proxyChannel.id();
                proxyChannel.writeAndFlush(response).addListener((future -> {
                    if (future.isSuccess()) {
                        log.debug("Requestor数据：" + msg.toString() +
                                        "\n Server--[{}]--ClientInternal--[{}]--ClientProxy>>[{}]>>Responsor"
                                , internalChannel.id(), channelId);
                    }
                }));
            }
            log.debug("Requestor数据：" + msg.toString() +
                            "\n Server>>[{}]>>ClientInternal--[{}]--ClientProxy--[{}]--Responsor"
                    , internalChannel.id(), proxyChannel.id());
            ByteBuf response = Unpooled.buffer();
            response.writeBytes((byte[]) msg.getData().get("data"));
            final ChannelId channelId = proxyChannel.id();
            proxyChannel.writeAndFlush(response).addListener((future -> {
                if (future.isSuccess()) {
                    log.debug("Requestor数据：" + msg.toString() +
                                    "\n Server--[{}]--ClientInternal--[{}]--ClientProxy>>[{}]>>Responsor"
                            , internalChannel.id(), channelId);
                }
            }));
        } else {
            //增加可靠性，proxy连接不存在时，重新建立配对并发送消息
            ProxyClient proxyClient = new ProxyClient();
            try {
                //启动代理服务
                proxyClient.init();
                ChannelFuture future = proxyClient.start();
                future.get();
                if (future.isSuccess()) {
                    ClientChannelGroup.addChannelPair(internalChannel, future.channel());
                    log.debug("建立连接: ClientInternal--[{}]--ClientProxy--[{}]--Responsor", internalChannel.id(), future.channel().id());
                    ClientChannelGroup.removeIdleInternalChannel(internalChannel);
                    ClientChannelGroup.addInternalChannel(internalChannel);
                    Channel proxyChannel = future.channel();
                    ByteBuf response = Unpooled.buffer();
                    response.writeBytes((byte[]) msg.getData().get("data"));
                    proxyChannel.writeAndFlush(response).addListener((sendFuture -> {
                        if (sendFuture.isSuccess()) {
                            log.debug("Requestor数据：" + msg.toString() +
                                            "\n Server--[{}]--ClientInternal--[{}]--ClientProxy>>[{}]>>Responsor"
                                    , internalChannel.id(), proxyChannel.id());
                        }
                    }));
                }
            } catch (Exception e) {
                e.printStackTrace();
                ctx.close();
            }
        }
    }
}
