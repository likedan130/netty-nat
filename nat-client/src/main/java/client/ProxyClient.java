package client;

import client.handler.ProxyClientHandler;
import core.cache.PropertiesCache;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * @Author wneck130@gmail.com
 * @Function netty代理客户端，用于连接被代理的服务
 */
public class ProxyClient extends Client {

    private Channel channel;

    public void init() {
        //加载配置文件
        cache = PropertiesCache.getInstance();
    }

    public ChannelFuture start() throws InterruptedException {
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
                ch.pipeline().addLast(new ProxyClientHandler());
            }
        });

        //连接服务器
        f = client.connect("192.168.0.158",
               27017).sync();
        return f;
        //阻塞主进程直到连接断开
//        f.channel().closeFuture().sync();
    }

    /**
     * 获取新建立的连接
     * @Param timeout 超时时间，单位毫秒
     * @return
     */
    public Channel getChannel(int timeout) throws Exception{
        long start = System.currentTimeMillis();
        while (true) {
            Thread.sleep(0);
            if (System.currentTimeMillis() - start >= timeout) {
                throw new Exception("连接被代理服务超时!!!");
            }
            if (channel != null) {
                return channel;
            }
        }
    }

}
