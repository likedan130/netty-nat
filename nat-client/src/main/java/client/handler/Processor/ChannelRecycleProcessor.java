package client.handler.Processor;

import client.group.ClientChannelGroup;
import core.entity.Frame;
import core.processor.Processor;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class ChannelRecycleProcessor implements Processor {

    @Override
    public void process(ChannelHandlerContext ctx, Frame msg) throws Exception {
        Channel internalChannel = ctx.channel();
        LocalDateTime localDateTime = LocalDateTime.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        log.debug("InternalChannel："+ internalChannel.id() + " 回收连接!!!");
//        System.out.println("[DEBUG] "+dtf.format(localDateTime)+" - "+ctx.channel().id()+"InternalChannel："+ internalChannel.id() + " 回收连接!!!");
        if (ClientChannelGroup.channelPairExist(internalChannel.id())) {
            ClientChannelGroup.printGroupState();
            Channel proxyChannel = ClientChannelGroup.getProxyByInternal(internalChannel.id());
            ClientChannelGroup.removeProxyChannel(proxyChannel);
            if (internalChannel ==  null) {
                log.error("与internalChannel："+internalChannel.id()+"配对的proxyChannel为null");
//                System.out.println("[DEBUG] "+dtf.format(localDateTime)+" - "+ctx.channel().id()+"internalChannel为null");
                return;
            }
            ClientChannelGroup.removeChannelPair(internalChannel.id(), ctx.channel().id());
            ClientChannelGroup.releaseInternalChannel(internalChannel);
        } else {
            log.error("internalChannel："+ internalChannel.id() + " 未找到配对关系!!!");
//            System.out.println("[DEBUG] "+dtf.format(localDateTime)+" - internalChannel："+ internalChannel.id() + " 未找到配对关系!!!");
            ClientChannelGroup.printGroupState();
        }
    }
}
