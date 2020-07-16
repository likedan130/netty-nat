package server.handler.processor;

import core.constant.FrameConstant;
import core.enums.CommandEnum;
import core.enums.NumberEnum;
import core.utils.BufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import server.Server;
import server.handler.SysServerhandler;

import java.util.concurrent.TimeUnit;

public class HeartbeatProcessor implements Processor {

    @Override
    public void process(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        //响应心跳
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(FrameConstant.pv);
        long serial = System.currentTimeMillis();
        byteBuf.writeLong(serial);
        byteBuf.writeByte(CommandEnum.CMD_HEARTBEAT.getCmd());
        byteBuf.writeShort(1);
        //计算校验和
        int vc = 0;
        for (byte byteVal : BufUtil.getArray(byteBuf)) {
            vc = vc + (byteVal & 0xFF);
        }
        byteBuf.writeByte(vc);
        ctx.writeAndFlush(byteBuf);
    }

    /**
     * 客户端心跳超时检测
     */
    public void timeoutDetection(ChannelHandlerContext ctx){
        //todo 定时心跳监听待完善
        Server.scheduledExecutor.scheduleAtFixedRate(() -> {
            long time = System.currentTimeMillis();
            System.out.println("执行");
            if(time != 0) {
                long endTime = System.currentTimeMillis();
                System.out.println("time:" + time / 1000);
                System.out.println("周期执行定时任务：" + endTime / 1000);
                int interval = (int) (endTime - time) / 1000;
                System.out.println("心跳时间间隔：" + interval);
                if (interval > NumberEnum.FIFTEEN.getValue()) {
                    System.out.println("关闭通道");
                    //关闭链路
                    ctx.close();
                    //关闭定时器
                    try {
                        Server.scheduledExecutor.wait();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        },15,15, TimeUnit.SECONDS);
    }
}
