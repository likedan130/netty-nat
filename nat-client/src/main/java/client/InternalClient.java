package client;

import core.cache.PropertiesCache;
import client.handler.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * @Author wneck130@gmail.com
 * @Function netty客户端，用于和代理程序的服务端建立连接，以连接池的方式存在，传输代理的业务数据
 */
public class InternalClient extends Client {

    public void init() {
        cache = PropertiesCache.getInstance();
    }

    /**
     * 启动指定数量的内部的连接
     * @throws Exception
     */
    public void start(int num) throws Exception{
        group = new NioEventLoopGroup();
        //定义线程组，处理读写和链接事件
        Bootstrap client = new Bootstrap();
        client.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ch.pipeline().addLast(new InternalClientHandler());
                    }
                });
        for (int i = 0;i < num; i++) {
            //连接服务器
            ChannelFuture future = client.connect(cache.get("internal.host"),
                    cache.getInt("internal.server.port")).sync();
            //阻塞主进程直到连接断开
            future.channel().closeFuture().sync();
        }
    }
}
