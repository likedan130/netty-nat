package server.internal.decoder;

import core.constant.FrameConstant;
import core.entity.Frame;
import core.utils.BufUtil;
import core.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author wneck130@gmail.com
 * @function 编码器，将POJO对象转化成字节流
 */
@Slf4j
public class PojoToByteEncoder extends MessageToByteEncoder<Frame> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Frame msg, ByteBuf out) throws Exception {
        out.writeByte(msg.getPv());
        out.writeLong(msg.getSerial());
        out.writeBytes(msg.getReq().getBytes(StandardCharsets.UTF_8));
        out.writeBytes(msg.getRes().getBytes(StandardCharsets.UTF_8));
        out.writeByte(msg.getCmd());
        if (msg.getData() == null) {
            out.writeInt(0);
        } else {
            //填充默认值
            out.writeInt(0);
            LinkedHashMap<String, Object> dataMap = msg.getData();
            int length = dataMap.entrySet().stream().map(Map.Entry::getValue).mapToInt(value -> {
                if (value instanceof Byte) {
                    out.writeByte((byte) value);
                    return 1;
                } else if (value instanceof byte[]) {
                    out.writeBytes((byte[]) value);
                    return ((byte[]) value).length;
                } else {
                    //TODO 待实现多种数据类型的字节转化
                    return 0;
                }
            }).sum();
            out.setBytes(FrameConstant.FRAME_LEN_INDEX, ByteUtil.fromInt(length));
            log.debug("发送消息:{}", ByteUtil.toHexString(BufUtil.getArray(out)));
        }
    }
}
