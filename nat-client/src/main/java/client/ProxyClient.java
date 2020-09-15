package client;

import client.handler.ProxyClientHandler;
import core.cache.PropertiesCache;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * @Author wneck130@gmail.com
 * @Function netty代理客户端，用于连接被代理的服务
 */
public class ProxyClient extends BaseClient {

    /**
     * 被代理服务的地址
     */
    private static String HOST = "proxy.client.host";

    /**
     * 被代理服务的端口
     */
    private static String PORT = "proxy.client.port";

    public void init() {
        //加载配置文件
        cache = PropertiesCache.getInstance();
    }

    public ChannelFuture start() throws Exception {
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
                        .addLast(new ProxyClientHandler());
            }
        });
        return client.connect(cache.get(HOST), cache.getInt(PORT));
    }

}
