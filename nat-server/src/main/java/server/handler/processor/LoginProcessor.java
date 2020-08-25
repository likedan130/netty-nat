package server.handler.processor;

import core.entity.Frame;
import core.processor.Processor;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author wneck130@gmail.com
 * @Function 登录命令处理器
 */
public class LoginProcessor implements Processor {
    private  final Logger log = LoggerFactory.getLogger(LoginProcessor.class);

    @Override
    public void process(ChannelHandlerContext ctx, Frame msg) throws Exception {
        //TODO 预留接入命令，在安全等级有需要的时候通过接入命令认证每一条连接
    }
}
