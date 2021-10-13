package client.internal;

import client.internal.decoder.ByteToPojoDecoder;
import client.internal.decoder.PojoToByteEncoder;
import core.netty.group.ClientChannelGroup;
import client.internal.handler.CustomEventHandler;
import client.internal.handler.InternalClientHandler;
import client.internal.handler.processor.constant.ProcessorEnum;
import core.constant.FrameConstant;
import core.entity.Frame;
import core.entity.Tunnel;
import core.netty.group.channel.strategy.constant.ForkStrategyEnum;
import core.netty.handler.MessageSendFilter;
import core.netty.handler.MessageReceiveFilter;
import core.netty.stater.client.BaseClient;
import core.netty.stater.client.NettyClient;
import core.properties.cache.PropertiesCache;
import core.properties.loader.YamlLoader;
import core.utils.ByteUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author wneck130@gmail.com
 * @Description:
 * @date 2021/9/29
 */
@Slf4j
public class InternalNettyClient extends BaseClient implements NettyClient {

    /**
     * 代理程序内部通信使用的地址
     */
    public static String HOST = "internal.server.host";
    /**
     * 代理程序内部通信使用的端口
     */
    public static String PORT = "internal.server.port";
    /**
     * 代理程序内部连接的初始化数量
     */
    public static String INIT_NUM = "internal.channel.init.num";
    /**
     * 心跳间隔，默认为1分钟
     */
    private static long HEARTBEAT_INTERVAL = 1L;

    public Bootstrap client = new Bootstrap();

    public static int initNum;

    /**
     * 连接延时，防止过多消耗系统资源
     */
    public int CONNECTION_DELAY = 1;

    public static void main(String[] args) throws Exception {
        InternalNettyClient internalClient = new InternalNettyClient();
        new YamlLoader().load(internalClient.getClass().getResource("/").getPath());
        internalClient.start();
        ClientChannelGroup.printGroupState();
    }

    @Override
    public void init() {
        cache = PropertiesCache.getInstance();
        this.host = cache.get(HOST);
        this.port = cache.getInt(PORT);
        group = new NioEventLoopGroup();
        //定义线程组，处理读写和链接事件
        client.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ch.pipeline().addFirst(new LengthFieldBasedFrameDecoder(FrameConstant.FRAME_MAX_BYTES,
                                FrameConstant.FRAME_LEN_INDEX, FrameConstant.FRAME_LEN_LEN))
                                .addLast(new IdleStateHandler(0, 0, HEARTBEAT_INTERVAL, TimeUnit.MINUTES))
                                .addLast(new ByteToPojoDecoder())
                                .addLast(new PojoToByteEncoder())
                                .addLast(new MessageReceiveFilter())
                                .addLast(new MessageSendFilter())
                                .addLast(new CustomEventHandler())
                                .addLast(new InternalClientHandler());
                    }
                });
    }

    @Override
    public void start() {
        init();
        initNum = cache.getInt(INIT_NUM);
        connect(initNum, host, port);
    }

    /**
     * 启动指定数量的内部的连接
     * 建立连接失败时，按照简单的延迟重连机制进行重连
     *
     * @throws Exception
     */
    public void connect(Integer num, String host, int port) {
        //连接服务器
        if (ClientChannelGroup.internalGroup.size() < num) {
            try {
                client.connect(host,port).sync().addListener((future -> {
                    if (future.isSuccess()) {
                        connect(num, host, port);
                        CONNECTION_DELAY = 1;
                    } else {
                        BaseClient.scheduledExecutor.schedule(() -> connect(num, host, port), CONNECTION_DELAY, TimeUnit.SECONDS);
                        CONNECTION_DELAY = CONNECTION_DELAY * 2;
                        log.error("启动internalClient失败，" + CONNECTION_DELAY + "秒后重试!!!");
                    }
                }));
            } catch (Exception e) {
                e.printStackTrace();
                log.error("connect to InternalSever error : ", e);
                BaseClient.scheduledExecutor.schedule(() -> connect(num, host, port), CONNECTION_DELAY, TimeUnit.SECONDS);
                CONNECTION_DELAY = CONNECTION_DELAY * 2;
                log.error("启动internalClient失败，" + CONNECTION_DELAY + "秒后重试!!!");
            }
        } else {
            //连接池启动完毕后开始注册隧道信息
            // 多次注册不会影响服务端的端口监听，防止有新增加的隧道信息需要注册，每次连接都重新同步隧道信息
            register();
        }

    }

    /**
     * 向服务端发送隧道注册信息
     */
    public void register() {
        //指定数量的连接创建完毕，向服务端注册tunnel信息
        try {
            //获取tunnel信息
            List<Object> tunnels = cache.getList("tunnel");
            for (int i = 0; i < tunnels.size(); i++) {
                LinkedHashMap<String, Object> tunnelMap = (LinkedHashMap<String, Object>) tunnels.get(i);
                Tunnel tunnel = new Tunnel();
                Integer serverPort = (Integer) tunnelMap.get("serverPort");
                tunnel.setServerPort(serverPort);
                tunnel.setClientHost((String) tunnelMap.get("clientHost"));
                tunnel.setClientPort((Integer) tunnelMap.get("clientPort"));
                ClientChannelGroup.addTunnel(serverPort, tunnel);

                Channel channel = ClientChannelGroup.forkChannel(ForkStrategyEnum.RANDOM);

                Frame frame = new Frame();
                frame.setReq(channel.id().asShortText());
                frame.setRes(FrameConstant.DEFAULT_CHANNEL_ID);
                frame.setCmd(ProcessorEnum.LOGIN.getCmd());
                LinkedHashMap<String, Object> data = new LinkedHashMap<>();
                String password = cache.get("password");
                data.put("passwordLen", ByteUtil.fromInt(password.length())[3]);
                data.put("password", password.getBytes(StandardCharsets.UTF_8));
                data.put("serverPort", new byte[]{ByteUtil.fromInt(tunnel.getServerPort())[2],
                        ByteUtil.fromInt(tunnel.getServerPort())[3]});
                String[] clientHostSegment = tunnel.getClientHost().split("\\.");
                data.put("clientHost", new byte[]{ByteUtil.fromInt(Integer.parseInt(clientHostSegment[0]))[3],
                        ByteUtil.fromInt(Integer.parseInt(clientHostSegment[1]))[3],
                        ByteUtil.fromInt(Integer.parseInt(clientHostSegment[2]))[3],
                        ByteUtil.fromInt(Integer.parseInt(clientHostSegment[3]))[3]});
                data.put("clientPort", new byte[]{ByteUtil.fromInt(tunnel.getClientPort())[2],
                        ByteUtil.fromInt(tunnel.getClientPort())[3]});
                frame.setData(data);
                channel.writeAndFlush(frame);
            }
        } catch (Exception e) {
            log.error("向服务端注册隧道失败!!!");
        }
    }
}
