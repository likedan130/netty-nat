package client.reconnection;

import client.SysClient;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author xian
 * data 2020/7/21
 * 监听启动连接，失败重连
 */
public class ConnectionListener implements ChannelFutureListener {
    private final Logger log = LoggerFactory.getLogger(ConnectionListener.class);
    private SysClient client = new SysClient();
    /**
     * 负责监听启动时连接失败，重新连接功能
     * @param channelFuture
     * @throws Exception
     */
    @Override
    public void operationComplete(ChannelFuture channelFuture) throws Exception {
        if (!channelFuture.isSuccess()) {
            final EventLoop loop = channelFuture.channel().eventLoop();
            loop.schedule(new Runnable() {
                @Override
                public void run() {
                    log.warn("服务端链接不上，开始重连操作...");
                    try{
                        client.init();
                        client.start();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }, 3L, TimeUnit.SECONDS);
        } else {
            log.debug("服务端链接成功...");
        }
    }
}
