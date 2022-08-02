package client.proxy;

import client.proxy.handler.ProxyClientHandler;
import client.proxy.handler.TcpInLogHandler;
import client.proxy.handler.TcpOutLogHandler;
import core.netty.stater.client.BaseClient;
import core.netty.stater.client.NettyClient;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wneck130@gmail.com
 * @Description:
 * @date 2021/10/8
 */
@Slf4j
public class ProxyNettyClient extends BaseClient implements NettyClient{

    @Override
    public void init() {
    }

    @Override
    public void start() {
        try {
            //通过Bootstrap启动服务端
            Bootstrap client = new Bootstrap();
            //定义线程组，处理读写和链接事件
            group = new NioEventLoopGroup();
            client.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) {
                            //加入自定义的handler
                            ch.pipeline()
                                    //inbound流日志记录
                                    .addLast(new TcpInLogHandler())
                                    //outbound流日志记录
                                    .addLast(new TcpOutLogHandler())
                                    .addLast(new ProxyClientHandler());
                        }
                    });
            log.debug("启动ProxyClient连接到{}：{}", host, port);
            future = client.connect(host, port).sync();
        } catch (Exception e) {
            log.error("启动ProxyClient连接到失败");
        }
    }
}
