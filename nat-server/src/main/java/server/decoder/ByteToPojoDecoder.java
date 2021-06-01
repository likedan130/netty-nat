package server.decoder;

import core.constant.FrameConstant;
import core.entity.Frame;
import core.utils.BufUtil;
import core.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author wneck130@gmail.com
 * @function 解码器，字节转Java对象
 */
@Slf4j
public class ByteToPojoDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < FrameConstant.FRAME_MIN_LEN) {
            log.error("invalidate msg : " + ByteUtil.toHexString(BufUtil.getArray(in)));
            ctx.close();
            return;
        }
        byte pv = in.readByte();
        long serial = in.readLong();
        byte cmd = in.readByte();
        int len = in.readInt();
        if (in.readableBytes() != len) {
            log.error("invalidate msg : " + ByteUtil.toHexString(BufUtil.getArray(in)));
            ctx.close();
            return;
        }
        Frame frame = new Frame();
        frame.setPv(pv);
        frame.setSerial(serial);
        frame.setCmd(cmd);
        frame.setLen(len);
        try {
            switch (cmd) {
                case 0x01:
                    break;
                case 0x02:
                    break;
                case 0x03:
                    break;
                case 0x04:
                    break;
                case (byte) 0xFF:
                    byte[] dataBytes = new byte[len];
                    in.readBytes(dataBytes);
                    Map<String, Object> data = new HashMap<>();
                    data.put("data", dataBytes);
                    frame.setData(data);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            log.error("invalidate msg : " + ByteUtil.toHexString(BufUtil.getArray(in)));
            ctx.close();
            return;
        }
        out.add(frame);
    }
}
