package server.proxy.handler;

import core.netty.group.ServerChannelGroup;
import core.netty.group.channel.strategy.constant.ForkStrategyEnum;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import static io.netty.handler.codec.http.HttpUtil.is100ContinueExpected;

/**
 * @author wneck130@gmail.com
 * @Description:
 * @date 2021/9/27
 */
public class HttpProxyServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        //处理100 continue请求
        if (is100ContinueExpected(msg)) {
            ctx.write(new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.CONTINUE));
        }
        //收到外部的msg消息，首先挑选一条internalChannel准备进行数据转发
        Channel internalChannel = ServerChannelGroup.forkChannel(ForkStrategyEnum.MIN_LOAD);
        //TODO 可在此添加代码修改http请求
        internalChannel.writeAndFlush(msg);
    }
}
