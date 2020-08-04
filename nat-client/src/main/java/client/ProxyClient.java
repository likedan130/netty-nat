package client;

import client.handler.ProxyClientHandler;
import core.cache.PropertiesCache;
import core.constant.NumberConstant;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.ArrayList;
import java.util.List;

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

    public List<Object> start() throws Exception {
        List<Object> list = new ArrayList<>();
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
        list.add(cache);
        list.add(client);
        return list;
    }


    /**
     * 获取新建立的连接
     * @Param timeout 超时时间，单位毫秒
     * @return
     */
    public Channel getChannel(int timeout) throws Exception{
        long start = System.currentTimeMillis();
        while (true) {
            Thread.sleep(NumberConstant.ZERO);
            if (System.currentTimeMillis() - start >= timeout) {
                throw new Exception("连接被代理服务超时!!!");
            }
            if (channel != null) {
                return channel;
            }
        }
    }

}
