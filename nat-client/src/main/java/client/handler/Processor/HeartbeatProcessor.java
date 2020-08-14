package client.handler.Processor;

import core.constant.FrameConstant;
import core.enums.CommandEnum;
import core.utils.BufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

public class HeartbeatProcessor implements Processor {

    @Override
    public void process(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        //接收到服务器的心跳响应后间隔10秒给服务器发送心跳命令
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(FrameConstant.pv);
        long serial = System.currentTimeMillis();
        byteBuf.writeLong(serial);
        byteBuf.writeByte(CommandEnum.CMD_HEARTBEAT.getCmd());
        byteBuf.writeShort(FrameConstant.VC_CODE_LEN);
        //计算校验和
        int vc = 0;
        for (byte byteVal : BufUtil.getArray(byteBuf)) {
            vc = vc + (byteVal & 0xFF);
        }
        byteBuf.writeByte(vc);
    }
}
