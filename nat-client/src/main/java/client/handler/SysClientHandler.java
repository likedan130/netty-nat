package client.handler;

import client.handler.Processor.ConnectionPoolProcessor;
import client.handler.Processor.HeartbeatProcessor;
import client.handler.Processor.LoginProcessor;
import core.constant.FrameConstant;
import core.detection.PublicDetectionHandler;
import core.enums.CommandEnum;
import core.utils.BufUtil;
import core.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;

import java.util.Date;

/**
 * @Author wneck130@gmail.com
 * @Function 代理程序的系统业务处理器
 */
public class SysClientHandler extends SimpleChannelInboundHandler<ByteBuf>{

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        System.out.println("SysClientHandler");
        //判断是否满足自定义协议
        if(PublicDetectionHandler.detection(msg)){
            return;
        }
        //协议中第10个字节为命令字，getByte中index从0开始
        byte cmd = msg.getByte(9);
        switch (cmd) {
            case (byte)0x01:
                System.out.println("服务端响应接入连接");
                new LoginProcessor().process(ctx, msg);
                break;
            case (byte)0x02:
                System.out.println("服务端响应心跳："+System.currentTimeMillis()/1000);
                new HeartbeatProcessor().process(ctx, msg);
                break;
            case (byte)0x03:
                System.out.println("接收服务端连接池命令");
                new ConnectionPoolProcessor().process(ctx, msg);
                break;
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("sysServer连接成功");
        //TCP连接建立后，马上发送登录信息给服务端
        byte[] password = "password".getBytes("UTF-8");
        int passwordLen = password.length;
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(FrameConstant.pv);
        long serial = System.currentTimeMillis();
        byteBuf.writeLong(serial);
        byteBuf.writeByte(CommandEnum.CMD_LOGIN.getCmd());
        byteBuf.writeShort(1 + 1 + passwordLen);
        byteBuf.writeByte(passwordLen);
        byteBuf.writeBytes(password);
        //计算校验和
        int vc = 0;
        for (byte byteVal : BufUtil.getArray(byteBuf)) {
            vc = vc + (byteVal & 0xFF);
        }
        System.out.println("client校验码："+ vc);
        byteBuf.writeByte(vc);
        System.out.println(ByteUtil.toHexString(BufUtil.getArray(byteBuf)));
        ctx.writeAndFlush(byteBuf);

        //发送心跳命令
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(FrameConstant.pv);
        buf.writeLong(System.currentTimeMillis());
        buf.writeByte(CommandEnum.CMD_HEARTBEAT.getCmd());
        buf.writeShort(1);
        //计算校验和
        int vc1 = 0;
        for (byte byteVal : BufUtil.getArray(buf)) {
            vc1 = vc1 + (byteVal & 0xFF);
        }
        buf.writeByte(vc1);
        ctx.writeAndFlush(buf);
    }



    //服务端断开触发
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("服务端停止时间是："+new Date());
    }
}
