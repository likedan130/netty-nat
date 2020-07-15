package client;

import core.cache.PropertiesCache;
import core.frame.loader.PropertiesLoader;
import client.handler.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * @Author wneck130@gmail.com
 * @Function netty客户端，用于和代理程序的服务端建立连接，满足系统业务
 */
public class SysClient extends Client {

    private static Bootstrap client = new Bootstrap();

    public void init() {
        //加载配置文件
        new PropertiesLoader().load(System.getProperty("user.dir"));
        cache = PropertiesCache.getInstance();
    }

    /**
     * 启动一条内部的连接
     * @throws Exception
     */
    public void start() throws Exception{
        group = new NioEventLoopGroup();
        client.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(65535,10,2))
                                .addLast(new IdleStateHandler(0, 0, 10))
                                //加入自定义的handler
                                .addLast(new SysClientHandler());
                    }
                });
        //连接服务器
        f = client.connect(cache.get("internal.host"),
                cache.getInt("internal.port")).sync();
        //阻塞主进程直到连接断开
        f.channel().closeFuture().sync();
    }

    public static void main(String[] args) throws Exception {
        SysClient sysClient = new SysClient();
        sysClient.init();
        sysClient.start();
    }
}
