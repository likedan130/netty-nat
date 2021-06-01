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
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @Author wneck130@gmail.com
 * @Function netty客户端，用于和代理程序的服务端建立连接，以连接池的方式存在，传输代理的业务数据
 */
@Slf4j
public class InternalClient extends BaseClient {

    /**
     * 连接延时，防止过多消耗系统资源
     */
    public static int CONNECTION_DELAY = 1;
    public static Bootstrap client = new Bootstrap();
    public static int initNum;
    /**
     * 客户端重连、扩展标志位，正在连接中时，对外部的连接建立命令静默
     */
    public static boolean connectingFlag = false;
    /**
     * 代理程序内部通信使用的地址
     */
    private static String HOST = "internal.server.host";
    /**
     * 代理程序内部通信使用的端口
     */
    private static String PORT = "internal.server.port";
    /**
     * 代理程序内部连接的初始化数量
     */
    private static String INIT_NUM = "internal.channel.init.num";
    /**
     * 心跳间隔，默认为1分钟
     */
    private static long HEARTBEAT_INTERVAL = 1L;

    public static boolean isChanging() {
        return connectingFlag;
    }

    public static void main(String[] args) throws Exception {
        InternalClient internalClient = new InternalClient();
        internalClient.init();
        internalClient.start();
        ClientChannelGroup.printGroupState();
    }

    public void init() throws Exception {
        new PropertiesLoader().load(System.getProperty("user.dir"));
//        new PropertiesLoader().load("E:\\songwei\\workspace\\netty-nat\\nat-client");
        group = new NioEventLoopGroup();
        //定义线程组，处理读写和链接事件
        client.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ch.pipeline().addFirst(new LengthFieldBasedFrameDecoder(FrameConstant.FRAME_MAX_BYTES,
                                FrameConstant.FRAME_LEN_INDEX, FrameConstant.FRAME_LEN_LEN))
                                .addLast(new ByteToPojoDecoder())
                                .addLast(new PojoToByteEncoder())
                                .addLast(new IdleStateHandler(0, 0, HEARTBEAT_INTERVAL, TimeUnit.MINUTES))
                                .addLast(new CustomEventHandler())
                                .addLast(new InternalClientHandler());
                    }
                });
    }

    /**
     * 启动内部连接
     *
     * @throws Exception
     */
    public void start() {
        cache = PropertiesCache.getInstance();
        initNum = cache.getInt(INIT_NUM);
        connect(initNum);
    }

    /**
     * 启动指定数量的内部的连接
     * 建立连接失败时，按照简单的延迟重连机制进行重连
     *
     * @throws Exception
     */
    public void connect(Integer num) {
        cache = PropertiesCache.getInstance();
        //连接服务器
        if (ClientChannelGroup.getIdleInternalGroupSize() < num) {
            connectingFlag = true;
            try {
                client.connect(cache.get(HOST),
                        cache.getInt(PORT)).addListener((future -> {
                    if (future.isSuccess()) {
                        connect(num);
                        CONNECTION_DELAY = 1;
                    } else {
                        BaseClient.scheduledExecutor.schedule(() -> connect(num), CONNECTION_DELAY, TimeUnit.SECONDS);
                        CONNECTION_DELAY = CONNECTION_DELAY * 2;
                        log.error("启动internalChannel失败，" + CONNECTION_DELAY + "秒后重试!!!");
                    }
                }));
            } catch (Exception e) {
                e.printStackTrace();
                log.error("connect to InternalSever error : ", e);
                BaseClient.scheduledExecutor.schedule(() -> connect(num), CONNECTION_DELAY, TimeUnit.SECONDS);
                CONNECTION_DELAY = CONNECTION_DELAY * 2;
                log.error("启动internalChannel失败，" + CONNECTION_DELAY + "秒后重试!!!");
            }
        } else {
            connectingFlag = false;
        }

    }
}
