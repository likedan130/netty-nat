package client.reconnection;

import core.constant.NumberConstant;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author xian
 * @data 2020/07/16
 * 重连检测，当发现当前的链路不稳定关闭之后，进行10次重连
 * 2的倍数增涨重连间隔时长
 */
@Sharable
public abstract class AbstractConnectionWatchdog extends ChannelInboundHandlerAdapter implements TimerTask, ChannelHandlerHolder {
    private final Logger log = LoggerFactory.getLogger(AbstractConnectionWatchdog.class);
    /**
     * 连接信息
     */
    private final Bootstrap bootstrap;
    /**
     * 定时重连
     */
    private final Timer timer;
    /**
     * 端口号
     */
    private final int port;
    /**
     * sysServer地址
     */
    private final String host;
    /**
     * 状态判断
     */
    private boolean reconnect;
    /**
     * 重连次数
     */
    private int attempts;


    public AbstractConnectionWatchdog(Bootstrap bootstrap, Timer timer, int port, String host, boolean reconnect) {
        this.bootstrap = bootstrap;
        this.timer = timer;
        this.port = port;
        this.host = host;
        this.reconnect = reconnect;
    }

    /**
     * 新客户端接入|客户端去和服务端连接成功时触发
     * channel链路每次active的时候，将其连接的次数重新☞ 0
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        log.info("当前链路已经激活了，重连尝试次数重新置为0");
        attempts = NumberConstant.ZERO;
        ctx.fireChannelActive();
    }

    //客户端断开触发
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("链接关闭");
        if(reconnect){
            log.info("链接关闭，将进行重连");
            if (attempts < NumberConstant.TEN) {
                attempts++;
                //2的倍数增涨重连间隔时长
                int timeout = NumberConstant.TWO << attempts;
                timer.newTimeout(this, timeout, TimeUnit.SECONDS);
            }
        }
        ctx.fireChannelInactive();
    }

    /**
     * HashedWheelTimer 实现 Timer 并重写newTimeout方法实现延时
     * 任务处理，newTimeout会执行 TimerTask run() 方法
     * @param timeout
     * @throws Exception
     */
    @Override
    public void run(Timeout timeout) throws Exception {

        ChannelFuture future;
        //bootstrap已经初始化好了，只需要将handler填入就可以了
        synchronized (bootstrap) {
            bootstrap.handler(new ChannelInitializer<Channel>() {

                @Override
                protected void initChannel(Channel ch) throws Exception {

                    ch.pipeline().addLast(handlers());
                }
            });
            future = bootstrap.connect(host,port);
        }
        //future对象
        future.addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture f) throws Exception {
                boolean succeed = f.isSuccess();
                //如果重连失败，则调用ChannelInactive方法，再次出发重连事件，一直尝试10次，如果失败则不再重连
                if (!succeed) {
                    log.warn("重连失败");
                    f.channel().pipeline().fireChannelInactive();
                }else{
                    log.info("重连成功");
                }
            }
        });
    }
}
