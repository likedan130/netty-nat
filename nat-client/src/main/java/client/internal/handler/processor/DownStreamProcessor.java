package client.internal.handler.processor;

import core.netty.group.ClientChannelGroup;
import client.internal.handler.processor.constant.ProcessorEnum;
import core.constant.FrameConstant;
import core.entity.Frame;
import core.netty.handler.processor.Processor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * @author wneck130@gmail.com
 * @Description: 数据下发处理器（cmd：0xEE），由proxyServer发起请求，proxyClient回复响应
 * @date 2021/10/8
 */
public class DownStreamProcessor implements Processor {

    @Override
    public Frame assemble(ByteBuf in) throws Exception {
        Frame frame = new Frame().quickHead(in);
        byte[] dataBytes = new byte[in.readableBytes()];
        in.readBytes(dataBytes);
        LinkedHashMap<String, Object> dataMap = new LinkedHashMap<>();
        dataMap.put("data", dataBytes);
        frame.setData(dataMap);
        return frame;
    }

    @Override
    public void request(ChannelHandlerContext ctx, Frame msg) throws Exception {
        //转发来自服务端的数据
        String clientChannelId = msg.getRes();
        Channel proxyClientChannel = ClientChannelGroup.findProxy(clientChannelId);
        ByteBuf downStream = Unpooled.buffer();
        downStream.writeBytes((byte[]) msg.getData().get("data"));
        proxyClientChannel.writeAndFlush(downStream).addListener(future -> {
            Frame response = new Frame();
            response.setPv(FrameConstant.RES_PV);
            response.setSerial(msg.getSerial());
            response.setReq(msg.getReq());
            response.setRes(msg.getRes());
            response.setCmd(msg.getCmd());
            response.setLen(1);
            LinkedHashMap<String, Object> dataMap = new LinkedHashMap<>();
            if (future.isSuccess()) {
                dataMap.put("result", FrameConstant.RESULT_SUCCESS);
            } else {
                dataMap.put("result", FrameConstant.RESULT_FAIL);
            }
            response.setData(dataMap);
            ctx.writeAndFlush(response);
        });
    }

    @Override
    public boolean supply(byte cmd) {
        return Objects.equals(ProcessorEnum.DOWN_STREAM.getCmd(), cmd);
    }
}
