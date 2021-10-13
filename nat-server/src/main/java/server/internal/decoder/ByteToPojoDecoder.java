package server.internal.decoder;

import core.constant.FrameConstant;
import core.entity.Frame;
import core.netty.handler.processor.ProcessorManager;
import core.utils.BufUtil;
import core.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import server.internal.handler.processor.constant.ProcessorEnum;

import java.util.List;

/**
 * @Author wneck130@gmail.com
 * @function 解码器，字节转Java对象
 */
@Slf4j
public class ByteToPojoDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < FrameConstant.FRAME_MIN_LEN) {
            log.error("无法解析的消息 : " + ByteUtil.toHexString(BufUtil.getArray(in)));
            ctx.close();
            return;
        }
        log.debug("收到消息:{}", ByteUtil.toHexString(BufUtil.getArray(in)));
        byte cmd = in.getByte(FrameConstant.FRAME_CMD_INDEX);
        Frame frame = ProcessorManager.getInstance(ProcessorEnum.getClassByCmd(cmd)).assemble(in);
        out.add(frame);
    }
}
