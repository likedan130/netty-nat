package client.handler.Processor;

import core.constant.FrameConstant;
import core.constant.NumberConstant;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * @Author wneck130@gmail.com
 * @Function 登录命令相应处理器
 */
public class LoginProcessor implements Processor {

    @Override
    public void process(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception{
        //可读长度不够，说明不符合协议，不做解析
        byte result = byteBuf.getByte(NumberConstant.TWELVE);
        if (result == FrameConstant.RESULT_FAIL) {
            //重新接入
        }
    }
}
