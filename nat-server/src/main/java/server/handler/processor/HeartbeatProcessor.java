package server.handler.processor;

import core.constant.FrameConstant;
import core.enums.CommandEnum;
import core.utils.BufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.Server;
import server.handler.SysServerhandler;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HeartbeatProcessor implements Processor {
    Logger log = LoggerFactory.getLogger(HeartbeatProcessor.class);
    @Override
    public void process(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        //响应心跳
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
        ctx.writeAndFlush(byteBuf);
    }

//    /**
//     * 客户端心跳超时检测
//     */
//    public void timeoutDetection(ChannelHandlerContext ctx){
//        //判断scheduledExecutor是否关闭
//        if(Server.scheduledExecutor.isShutdown()){
//            //创建一个单线程执行器
//            Server.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
//        }
//        //执行周期任务，首次延迟15秒，后期每次间隔15秒执行
//        Server.scheduledExecutor.scheduleAtFixedRate(() -> {
//            //得到心跳发送时间
//            long time = SysServerhandler.time;
//            //首次连接不执行
//            if(time != 0L) {
//                long endTime = System.currentTimeMillis();
//                int interval = (int) (endTime - time) / 1000;
//                //如果间隔时间大于15秒则关闭channel
//                if (interval > FrameConstant.HEARTBEAT_TIMEOUT) {
//                    log.debug("关闭通道");
//                    //关闭链路
//                    ctx.close();
//                    //关闭
//                    Server.scheduledExecutor.shutdown();
//                }
//            }
//        }, FrameConstant.HEARTBEAT_TIMEOUT, FrameConstant.HEARTBEAT_TIMEOUT, TimeUnit.SECONDS);
//    }
}
