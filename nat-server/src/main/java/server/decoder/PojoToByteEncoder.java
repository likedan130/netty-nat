package server.decoder;

import core.entity.Frame;
import core.enums.CommandEnum;
import core.utils.BufUtil;
import core.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * @author wneck130@gmail.com
 * @function POJO对象编码器，将POJO对象转化成字节流
 */
@Slf4j
public class PojoToByteEncoder extends MessageToByteEncoder<Frame> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Frame msg, ByteBuf out) throws Exception {
        out.writeByte(msg.getPv());
        out.writeLong(msg.getSerial());
        out.writeByte(msg.getCmd());
        if (msg.getData() == null || msg.getData().get("data") == null) {
            out.writeInt(0);
        } else {
            out.writeInt(((byte[])msg.getData().get("data")).length);
            out.writeBytes(((byte[])msg.getData().get("data")));
        }
        if (msg.getCmd() == CommandEnum.CMD_DATA_TRANSFER.getCmd()) {
            byte[] print = Arrays.copyOf((byte[])msg.getData().get("data"), 5);
            log.debug("InternalServer:"+ctx.channel().id()+"发送数据：" + ByteUtil.toHexString(print));
        }
    }
}
