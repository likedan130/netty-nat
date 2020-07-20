package client.handler.Processor;

import core.constant.FrameConstant;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * @Author wneck130@gmail.com
 * @Function 登录命令相应处理器
 */
public class LoginProcessor implements Processor {

    private static byte SUCCESS_RESULT = 0x00;

    @Override
    public void process(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception{
        //可读长度不够，说明不符合协议，不做解析
        byte result = byteBuf.getByte(12);
        if (result == FrameConstant.RESULT_FAIL) {
            //重新接入
        }
    }
}
