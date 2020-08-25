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

        //根据internalChannel找到对应的proxyChannel
        if (ServerChannelGroup.channelPairExist(ctx.channel().id())) {
            ByteBuf response = Unpooled.buffer();
            response.writeBytes((byte[])msg.getData().get("data"));
            Channel proxyChannel = ServerChannelGroup.getProxyByInternal(ctx.channel().id());
            proxyChannel.writeAndFlush(response).addListener((future -> {
                if (future.isSuccess()) {
                    LocalDateTime localDateTime = LocalDateTime.now();
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
                    log.debug("proxyChannel："+ctx.channel().id()+"回复数据" + ByteUtil.toHexString(Arrays.copyOf((byte[])msg.getData().get("data"), 5)));
//                    System.out.println("[DEBUG] "+dtf.format(localDateTime)+" - "+ctx.channel().id()+" proxyChannel回复数据");
                }
            }));
        } else {
            //proxy连接可能已经被主动断开，忽略本条消息
            return;
        }
    }
}
