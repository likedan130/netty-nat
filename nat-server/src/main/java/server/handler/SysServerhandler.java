package server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import server.group.ServerChannelGroup;
import server.handler.processor.ConnectionPoolProcessor;
import server.handler.processor.HeartbeatProcessor;
import server.handler.processor.LoginProcessor;

import java.util.Date;

public class SysServerhandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        byte cmd = msg.getByte(9);

        switch (cmd) {
            //接入命令
            case 0x01:
                new LoginProcessor().process(ctx, msg);
                break;
            //心跳命令
            case 0x02:
                System.out.println("接收到客户心跳："+System.currentTimeMillis()/1000);
                new HeartbeatProcessor().process(ctx, msg);
//                new HeartbeatProcessor().timeoutDetection(ctx);
                break;
            //建立连接池命令
            case 0x03:
                new ConnectionPoolProcessor().process(ctx, msg);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("客户端组："+ServerChannelGroup.getSysChannel());
        System.out.println("sysClient连接成功");
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("客户端停止时间是："+new Date());
        super.channelInactive(ctx);
    }
}
