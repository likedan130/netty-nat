package client.handler.Processor;

import client.Client;
import core.constant.FrameConstant;
import core.constant.NumberConstant;
import core.enums.CommandEnum;
import core.utils.BufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import java.util.concurrent.TimeUnit;

public class HeartbeatProcessor implements Processor {

    @Override
    public void process(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        //接收到服务器的心跳响应后间隔10秒给服务器发送心跳命令
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(FrameConstant.pv);
        long serial = System.currentTimeMillis();
        byteBuf.writeLong(serial);
        byteBuf.writeByte(CommandEnum.CMD_HEARTBEAT.getCmd());
        byteBuf.writeShort(NumberConstant.ONE);
        //计算校验和
        int vc = NumberConstant.ZERO;
        for (byte byteVal : BufUtil.getArray(byteBuf)) {
            vc = vc + (byteVal & 0xFF);
        }
        byteBuf.writeByte(vc);
        //10秒延迟发送
        Client.scheduledExecutor.schedule(() -> ctx.writeAndFlush(byteBuf), NumberConstant.TEN, TimeUnit.SECONDS);
    }
}
