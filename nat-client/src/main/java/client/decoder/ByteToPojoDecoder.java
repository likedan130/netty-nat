package client.decoder;

import core.constant.FrameConstant;
import core.entity.Frame;
import core.utils.BufUtil;
import core.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
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
                    LocalDateTime localDateTime = LocalDateTime.now();
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
                    log.debug("InternalClient:"+ctx.channel().id()+"收到心跳指令");
//                    System.out.println("[DEBUG] "+dtf.format(localDateTime)+" - "+ctx.channel().id()+"  InternalClient收到心跳指令");
                    break;
                case 0x03:
                    localDateTime = LocalDateTime.now();
                    dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
//                    System.out.println("[DEBUG] "+dtf.format(localDateTime)+" - "+ctx.channel().id()+"  InternalClient收到建立代理连接指令");
                    log.debug("InternalClient:"+ctx.channel().id()+"收到建立代理连接指令");
                    break;
                case 0x04:
                    break;
                case (byte)0xFF:
                    byte[] dataBytes = new byte[len];
                    in.readBytes(dataBytes);
                    Map<String, Object> data = new HashMap<>();
                    data.put("data", dataBytes);
                    frame.setData(data);
                    localDateTime = LocalDateTime.now();
                    dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
//                    System.out.println("[DEBUG] "+dtf.format(localDateTime)+" - "+ctx.channel().id()+"  InternalClient收到数据："+ ByteUtil.toHexString(Arrays.copyOf(dataBytes, 5)));
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
