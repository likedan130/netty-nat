package client.internal.handler.processor;

import core.netty.group.ClientChannelGroup;
import client.internal.handler.processor.constant.ProcessorEnum;
import client.proxy.ProxyNettyClient;
import core.constant.FrameConstant;
import core.entity.Frame;
import core.netty.handler.processor.Processor;
import core.utils.BufUtil;
import core.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.UnknownFormatConversionException;

/**
 * @Author wneck130@gmail.com
 * @function 预创建连接命令处理器（cmd:0x03），命令由proxyServer发起请求，proxyClient回复响应
 */
@Slf4j
public class PreConnectProcessor implements Processor {

    @Override
    public Frame assemble(ByteBuf in) throws Exception {
        try {
            //解析协议公共部分
            Frame frame = new Frame().quickHead(in);
            int hostSegment1 = in.readByte() & 0xFF;
            int hostSegment2 = in.readByte() & 0xFF;
            int hostSegment3 = in.readByte() & 0xFF;
            int hostSegment4 = in.readByte() & 0xFF;
            String host = new StringBuilder().append(hostSegment1)
                    .append(".")
                    .append(hostSegment2)
                    .append(".")
                    .append(hostSegment3)
                    .append(".")
                    .append(hostSegment4).toString();
            int port = in.readUnsignedShort();
            LinkedHashMap<String, Object> dataMap = new LinkedHashMap<>();
            dataMap.put("host", host);
            dataMap.put("port", port);
            frame.setData(dataMap);
            return frame;
        } catch (Exception e) {
            log.error("无法解析的消息: " + ByteUtil.toHexString(BufUtil.getArray(in)));
            throw new UnknownFormatConversionException("无法解析的消息!!!");
        }
    }

    @Override
    public void request(ChannelHandlerContext ctx, Frame msg) throws Exception {
        try {
            //收到服务器的命令后主动建立与被代理服务之间的连接
            String host = (String) msg.getData().get("host");
            int port = (Integer) msg.getData().get("port");
            ProxyNettyClient proxyNettyClient = new ProxyNettyClient();
            proxyNettyClient.setHost(host);
            proxyNettyClient.setPort(port);
            proxyNettyClient.start();
            ChannelFuture future = proxyNettyClient.getFuture();
            //创建失败
            if (future == null) {
                response(ctx, msg, FrameConstant.RESULT_FAIL, FrameConstant.DEFAULT_CHANNEL_ID);
                return;
            }
            //创建成功后缓存配对关系，发送响应消息
            ClientChannelGroup.addChannelPair(msg.getReq(), future.channel().id().asShortText());
            log.debug("创建channelPair：[serverChannel：{}], [clientChannel：{}]", msg.getReq(),
                    future.channel().id().asShortText());
            response(ctx, msg, FrameConstant.RESULT_SUCCESS, future.channel().id().asShortText());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("预创建连接请求异常!!!");
        }
    }

    /**
     * 拼接响应内容
     * @param ctx
     * @param msg
     * @param result
     */
    public void response(ChannelHandlerContext ctx, Frame msg, byte result, String proxyChannelId) {
        Frame response = new Frame();
        response.setPv(FrameConstant.RES_PV);
        response.setSerial(msg.getSerial());
        response.setReq(msg.getReq());
        response.setRes(proxyChannelId);
        response.setLen(1);
        response.setCmd(msg.getCmd());
        LinkedHashMap<String, Object> dataMap = new LinkedHashMap<>();
        dataMap.put("result", result);
        response.setData(dataMap);
        ctx.writeAndFlush(response);
    }

    @Override
    public boolean supply(byte cmd) {
        return Objects.equals(ProcessorEnum.PRE_CONNECT, cmd);
    }
}
