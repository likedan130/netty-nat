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
import java.util.concurrent.Executors;
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
        //判断scheduledExecutor是否关闭
        if(Server.scheduledExecutor.isShutdown()){
            //创建一个单线程执行器
            Server.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        }
        //执行周期任务，首次延迟15秒，后期每次间隔15秒执行
        Server.scheduledExecutor.scheduleAtFixedRate(() -> {
            //得到心跳发送时间
            long time = SysServerhandler.time;
            System.out.println("执行");
            //首次连接不执行
            if(time != 0L) {
                long endTime = System.currentTimeMillis();
                System.out.println("time:" + time / 1000);
                System.out.println("周期执行定时任务：" + endTime / 1000);
                int interval = (int) (endTime - time) / 1000;
                System.out.println("心跳时间间隔：" + interval);
                //如果间隔时间大于15秒则关闭channel
                if (interval > NumberEnum.HEART_TATE_INTERVAL.getType()) {
                    System.out.println("关闭通道");
                    //关闭链路
                    ctx.close();
                    //关闭
                    Server.scheduledExecutor.shutdown();
                }
            }
        },15,15, TimeUnit.SECONDS);
    }
}
