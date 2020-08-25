package client.handler.Processor;

import core.entity.Frame;
import core.processor.Processor;
import io.netty.channel.ChannelHandlerContext;

/**
 * @Author wneck130@gmail.com
 * @Function 登录命令相应处理器
 */
public class LoginProcessor implements Processor {

    @Override
    public void process(ChannelHandlerContext ctx, Frame byteBuf) throws Exception {
        //TODO 预留接入命令，在安全等级有需要的时候通过接入命令认证每一条连接
    }
}
