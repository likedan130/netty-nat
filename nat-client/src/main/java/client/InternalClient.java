package client;

import core.cache.PropertiesCache;
import client.handler.*;
import core.constant.NumberConstant;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

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
            startClient(num,cache,client);
    }
    private void startClient(int num,PropertiesCache cache,Bootstrap client) throws Exception{
        //连接服务器
        if(num > NumberConstant.ZERO) {
            client.connect(cache.get("internal.host"),
                    cache.getInt("internal.server.port")).sync().addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(Future<? super Void> future) throws Exception {
                    if (future.isSuccess()) {
                        startClient(num-1, cache, client);
                    }
                }
            });
        }
    }
}
