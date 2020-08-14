package client.handler.Processor;

import client.Client;
import core.constant.FrameConstant;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.TimeUnit;

/**
 * @Author wneck130@gmail.com
 * @Function 登录命令相应处理器
 */
public class LoginProcessor implements Processor {

    @Override
    public void process(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception{
        //可读长度不够，说明不符合协议，不做解析
        byte result = byteBuf.getByte(FrameConstant.FRAME_RESULT_INDEX);
        if (result == FrameConstant.RESULT_FAIL) {
            //重新接入
        }
        //10秒延迟发送
        Client.scheduledExecutor.scheduleAtFixedRate(() -> ctx.writeAndFlush(byteBuf), 0L,
                FrameConstant.HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
    }
}
