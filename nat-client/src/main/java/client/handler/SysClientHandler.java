package client.handler;

import client.SysClient;
import client.handler.Processor.HeartbeatProcessor;
import client.handler.Processor.LoginProcessor;
import core.enums.CommandEnum;
import core.utils.BufUtil;
import core.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @Author wneck130@gmail.com
 * @Function 代理程序的系统业务处理器
 */
public class SysClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static byte pv = (byte)0xAA;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        //协议中第10个字节为命令字，getByte中index从0开始
        byte cmd = msg.getByte(9);
        switch (cmd) {
            case (byte)0x01:
                new LoginProcessor().process(ctx, msg);
                break;
            case (byte)0x02:
                new HeartbeatProcessor().process(ctx, msg);
                break;
        }

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //TCP连接建立后，马上发送登录信息给服务端
        byte[] password = "pasword".getBytes("UTF-8");
        int passwordLen = password.length;
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(pv);
        Long serial = System.currentTimeMillis();
        byteBuf.writeLong(serial);
        byteBuf.writeByte(CommandEnum.CMD_LOGIN.getCmd());
        byteBuf.writeShort(1 + passwordLen + 1);
        byteBuf.writeByte(passwordLen);
        byteBuf.writeBytes(password);
        //计算校验和
        int vc = 0;
        for (byte byteVal : BufUtil.getArray(byteBuf)) {
            vc = vc + (byteVal & 0xFF);
        }
        byteBuf.writeByte(vc);
        System.out.println(ByteUtil.toHexString(BufUtil.getArray(byteBuf)));
        ctx.writeAndFlush(byteBuf);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //tcp连接断开后马上重新创建连接
        SysClient sysClient = new SysClient();
        sysClient.init();
        sysClient.start();
    }
}
