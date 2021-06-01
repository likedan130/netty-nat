package client.decoder;

import core.entity.Frame;
import core.enums.CommandEnum;
import core.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * @Author wneck130@gmail.com
 * @function 编码器，Java对象转字节
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
            out.writeInt(((byte[]) msg.getData().get("data")).length);
            out.writeBytes(((byte[]) msg.getData().get("data")));
        }
        if (msg.getCmd() == CommandEnum.CMD_DATA_TRANSFER.getCmd()) {
            byte[] print = Arrays.copyOf((byte[]) msg.getData().get("data"), 5);
            log.debug("InternalClient" + ctx.channel().id() + "发送数据：" + ByteUtil.toHexString(print));
        }
    }
}
