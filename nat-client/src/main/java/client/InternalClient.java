package client;

import client.decoder.ByteToPojoDecoder;
import client.decoder.PojoToByteEncoder;
import client.group.ClientChannelGroup;
import client.handler.CustomEventHandler;
import client.handler.InternalClientHandler;
import core.cache.PropertiesCache;
import core.constant.FrameConstant;
import core.frame.loader.PropertiesLoader;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * @Author wneck130@gmail.com
 * @Function netty客户端，用于和代理程序的服务端建立连接，以连接池的方式存在，传输代理的业务数据
 */
public class InternalClient extends Client {

    /**
     * 连接延时，防止过多消耗系统资源
     */
    public static int CONNECTION_DELAY = 1;

    public static Bootstrap client = new Bootstrap();

    public static int initNum;

    /**
     * 客户端重连、扩展标志位
     */
    public static boolean changeFlag = false;

    public static boolean isChanging() {
        return changeFlag;
    }

    public void init() {
        new PropertiesLoader().load(System.getProperty("user.dir"));
        group = new NioEventLoopGroup();
        //定义线程组，处理读写和链接事件
        client.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ch.pipeline().addFirst(new LengthFieldBasedFrameDecoder(FrameConstant.FRAME_MAX_BYTES,
                                FrameConstant.FRAME_LEN_INDEX, FrameConstant.FRAME_LEN_LEN))
//                                .addLast(new IdleStateHandler(0,0,1))
                                .addLast(new ByteToPojoDecoder())
                                .addLast(new PojoToByteEncoder())
//                                .addLast(new CustomEventHandler())
                                .addLast(new InternalClientHandler());
                    }
                });
    }
//
//    /**
//     * 启动单条连接校验与服务器之间的通信
//     */
//    public void reConnect() {
//        client.connect(cache.get("internal.host"), cache.getInt("internal.server.port")).addListener((future -> {
//            if (future.isSuccess()) {
//                start();
//                CONNECTION_DELAY = 1;
//                System.out.println("尝试重连成功!!!");
//            } else {
//                Client.scheduledExecutor.schedule(() -> reConnect(), CONNECTION_DELAY, TimeUnit.SECONDS);
//                CONNECTION_DELAY = CONNECTION_DELAY * 2;
//                System.out.println("尝试重连失败，当前重连延迟"+CONNECTION_DELAY+"!!!");
//            }
//        }));
//    }

    /**
     * 启动内部连接
     * @throws Exception
     */
    public void start() {
        cache = PropertiesCache.getInstance();
        initNum = cache.getInt("internal.channel.init.num");
        connect(initNum);
    }


    /**
     * 启动指定数量的内部的连接
     * @throws Exception
     */
    public void connect(Integer num) {
        cache = PropertiesCache.getInstance();
        //连接服务器
        if (ClientChannelGroup.getIdleInternalGroupSize() < num) {
            changeFlag = true;
            try {
                client.connect(cache.get("internal.host"),
                        cache.getInt("internal.server.port")).addListener((future -> {
                    if (future.isSuccess()) {
                        connect(num);
                        CONNECTION_DELAY = 1;
//                        System.out.println("启动internalChannel成功!!!" + ClientChannelGroup.getIdleInternalGroupSize());
                    } else {
                        Client.scheduledExecutor.schedule(() -> connect(num), CONNECTION_DELAY, TimeUnit.SECONDS);
                        CONNECTION_DELAY = CONNECTION_DELAY * 2;
//                        System.out.println("启动internalChannel失败，"+CONNECTION_DELAY+"秒后重试!!!");
                        log.error("启动internalChannel失败，"+CONNECTION_DELAY+"秒后重试!!!");
                    }
                }));
            } catch (Exception e) {
                e.printStackTrace();
                log.error("connect to InternalSever error : ", e);
                Client.scheduledExecutor.schedule(() -> connect(num), CONNECTION_DELAY, TimeUnit.SECONDS);
                CONNECTION_DELAY = CONNECTION_DELAY * 2;
                log.error("启动internalChannel失败，"+CONNECTION_DELAY+"秒后重试!!!");
//                System.out.println("启动internalChannel失败，"+CONNECTION_DELAY+"秒后重试!!!");
            }
        } else {
            changeFlag = false;
        }

    }


    public static void main(String[] args) {
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
        InternalClient internalClient = new InternalClient();
        internalClient.init();
        internalClient.start();
        ClientChannelGroup.printGroupState();
    }
}
