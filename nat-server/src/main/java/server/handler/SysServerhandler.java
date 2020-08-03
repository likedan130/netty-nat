package server.handler;

import core.constant.NumberConstant;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import server.handler.processor.ConnectionPoolProcessor;
import server.handler.processor.HeartbeatProcessor;
import server.handler.processor.LoginProcessor;

public class SysServerhandler extends SimpleChannelInboundHandler<ByteBuf> {
    /**
     * 接收心跳时间
     */
    public static long time = 0L;
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        byte cmd = msg.getByte(NumberConstant.NINE);
        switch (cmd) {
            //接入命令
            case 0x01:
                new LoginProcessor().process(ctx, msg);
                //15秒心跳检测
                new HeartbeatProcessor().timeoutDetection(ctx);
                break;
            //心跳命令
            case 0x02:
                time = System.currentTimeMillis();
                new HeartbeatProcessor().process(ctx, msg);
                break;
            //建立连接池命令
            case 0x03:
                new ConnectionPoolProcessor().process(ctx, msg);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

}
