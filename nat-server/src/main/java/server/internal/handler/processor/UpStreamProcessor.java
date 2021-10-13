package server.internal.handler.processor;

import core.constant.FrameConstant;
import core.entity.Frame;
import core.netty.group.ServerChannelGroup;
import core.netty.handler.processor.Processor;
import core.utils.BufUtil;
import core.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import server.internal.handler.processor.constant.ProcessorEnum;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.UnknownFormatConversionException;

/**
 * @author wneck130@gmail.com
 * @Description: 上行数据处理器（cmd:0xFF），命令由proxyClient发起请求，proxyServer回复响应
 * @date 2021/9/29
 */
@Slf4j
public class UpStreamProcessor implements Processor {

    @Override
    public Frame assemble(ByteBuf in) throws Exception {
        try {
            Frame frame = new Frame().quickHead(in);
            //解析协议data部分
            byte[] dataBytes = new byte[in.readableBytes()];
            in.readBytes(dataBytes);
            //生成数据帧
            LinkedHashMap<String, Object> dataMap = new LinkedHashMap<>();
            dataMap.put("data", dataBytes);
            frame.setData(dataMap);
            return frame;
        } catch (Exception e) {
            log.error("无法解析的消息: " + ByteUtil.toHexString(BufUtil.getArray(in)));
            throw new UnknownFormatConversionException("无法解析的消息!!!");
        }
    }

    @Override
    public void request(ChannelHandlerContext ctx, Frame msg) throws Exception {
        //将消息转发给对应的外部请求方
        Channel proxyChannel = ServerChannelGroup.findProxy(msg.getRes());
        ByteBuf upStream = Unpooled.buffer();
        upStream.writeBytes((byte[])msg.getData().get("data"));
        proxyChannel.writeAndFlush(upStream).addListener(future -> {
            //根据发送结果回写响应
            Frame response = new Frame();
            response.setPv(FrameConstant.RES_PV);
            response.setSerial(msg.getSerial());
            response.setCmd(msg.getCmd());
            response.setLen(FrameConstant.FRAME_RESULT_LEN);
            LinkedHashMap<String, Object> dataMap = new LinkedHashMap<>(1);
            proxyChannel.writeAndFlush(response);
            if (future.isSuccess()) {
                dataMap.put("result", FrameConstant.RESULT_SUCCESS);
            } else {
                log.error("向外部请求发送数据失败:", future.cause());
                dataMap.put("result", FrameConstant.RESULT_FAIL);
            }
            response.setData(dataMap);
            Channel internalChannel = ServerChannelGroup.forkChannel(proxyChannel.id().asShortText());
            internalChannel.writeAndFlush(response);
        });
    }

    @Override
    public boolean supply(byte cmd) {
        return Objects.equals(ProcessorEnum.UP_STREAM.getCmd(), cmd);
    }
}
