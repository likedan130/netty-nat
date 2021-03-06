package client.group;

import client.BaseClient;
import client.InternalClient;
import client.ProxyClient;
import core.cache.PropertiesCache;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author wneck130@gmail.com
 * @function 客户端连接组，管理所有客户端的TCP连接
 */
@Slf4j
public class ClientChannelGroup {

    private static PropertiesCache cache = PropertiesCache.getInstance();

    private static Long CONNECT_PROXY_TIMEOUT = 3L;

    /**
     * channel对，将服务端与客户端的channel进行配对，便于消息转发
     * key为连接池中内部连接的channelId，value为proxy服务的channelId
     */
    private static Map<ChannelId, ChannelId> channelPair = new ConcurrentHashMap<>();

    /**
     * 被代理服务的channel组
     */
    private static ChannelGroup proxyGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * 系统内部连接池的channel组
     */
    private static ChannelGroup internalGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * 内部服务的channel组
     */
    private static ChannelGroup idleInternalGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public static ChannelGroup getIdleInternalGroup() {
        return idleInternalGroup;
    }

    public static int getIdleInternalGroupSize() {
        return idleInternalGroup.size();
    }

    public static Map<ChannelId, ChannelId> getChannelPair() {
        return channelPair;
    }

    public static ChannelGroup getInternalGroup() {
        return internalGroup;
    }

    public static void addProxyChannel(Channel channel) {
        proxyGroup.add(channel);
    }

    public static void removeProxyChannel(Channel channel) {
        proxyGroup.remove(channel);
    }

    public static void addInternalChannel(Channel channel) {
        internalGroup.add(channel);
    }

    public static void removeInternalChannel(Channel channel) throws Exception {
        internalGroup.remove(channel);
    }

    public static void removeIdleInternalChannel(Channel channel) throws Exception {
        idleInternalGroup.remove(channel);
        //检查空闲连接是否小于最小空闲连接数
        if (idleInternalGroup.size() < cache.getInt("internal.channel.min.idle.num")) {
            InternalClient internalClient = new InternalClient();
            BaseClient.threadPoolExecutor.execute(() -> {
                if (!InternalClient.isChanging()) {
                    internalClient.connect(5);
                    log.debug("InternalChannel当前连接数{}少于{}，触发连接池扩容!!!", idleInternalGroup.size(),
                            cache.getInt("internal.channel.min.idle.num"));
                }
            });
        }
    }

    public static void addIdleInternalChannel(Channel channel) {
        idleInternalGroup.add(channel);
        //检查空闲连接是否大于最大空闲连接数，成立则关闭当前连接
        if (idleInternalGroup.size() > cache.getInt("internal.channel.max.idle.num")) {
            log.debug("InternalChannel当前连接数{}超过{}，触发连接池缩减!!!", idleInternalGroup.size(),
                    cache.getInt("internal.channel.max.idle.num"));
            channel.close();
        }
    }

    public static void addChannelPair(Channel internalChannel, Channel proxyChannel) {
        channelPair.put(internalChannel.id(), proxyChannel.id());
    }

    public static void removeChannelPair(ChannelId internalChannelId, ChannelId proxyChannelId) throws Exception {
        channelPair.remove(internalChannelId, proxyChannelId);
    }

    public static void removeChannelPair(ChannelId internalChannelId) throws Exception {
        channelPair.remove(internalChannelId);
    }

    /**
     * 释放占用的连接池连接
     *
     * @param channel
     */
    public static void releaseInternalChannel(Channel channel) {
        internalGroup.remove(channel);
        addIdleInternalChannel(channel);
    }

    /**
     * 判断当前channel是否已经建立channelPair，没有建立channelPair的channel无法进行转发
     *
     * @param channelId
     * @return
     */
    public static boolean channelPairExist(ChannelId channelId) {
        if (channelPair.isEmpty()) {
            return Boolean.FALSE;
        }
        if (channelPair.containsKey(channelId)) {
            return Boolean.TRUE;
        }
        if (channelPair.containsValue(channelId)) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /**
     * 根据内部channleId获取代理channel
     *
     * @param channelId
     * @return
     */
    public static Channel getProxyByInternal(ChannelId channelId) throws Exception {
        ChannelId proxyChannelId = channelPair.get(channelId);
        return proxyGroup.find(proxyChannelId);
    }

    /**
     * 根据代理channleId获取内部channel
     *
     * @param channelId
     * @return
     */
    public static Channel getInternalByProxy(ChannelId channelId) throws Exception {
        List<ChannelId> result = channelPair.entrySet().stream()
                .filter(e -> Objects.equals(e.getValue(), channelId))
                .map((x) -> x.getKey())
                .collect(Collectors.toList());
        if (result.isEmpty() || result.size() != 1) {
            log.error("找不到proxyChannel" + channelId + "!!!");
            return null;
        }
        return internalGroup.find(result.get(0));
    }

    public static void printGroupState() {
        log.debug("当前channel情况：");
        log.debug("IdleInternalChannel数量：" + ClientChannelGroup.getIdleInternalGroup().size());
        log.debug("InternalChannel数量：" + ClientChannelGroup.getInternalGroup().size());
        log.debug("当前channelPair：");
        ClientChannelGroup.getChannelPair().entrySet().stream().forEach((entry) -> {
            log.debug("[InternalChannel：" + entry.getKey() + ", ProxyChannel：" + entry.getValue() + "]");
        });
    }

    /**
     * 统一创建与被代理服务间通信的方法，阻塞直到连接建立完成
     *
     * @param ctx
     */
    public static ChannelFuture connectToProxy(ChannelHandlerContext ctx) throws Exception {
        //收到服务器的命令后主动建立与被代理服务之间的连接
        ProxyClient proxyClient = new ProxyClient();
        //启动代理服务
        proxyClient.init();
        ChannelFuture future = proxyClient.start();
        future.get(CONNECT_PROXY_TIMEOUT, TimeUnit.SECONDS);
        Channel internalChannel = ctx.channel();
        if (future.isSuccess()) {
            ClientChannelGroup.addChannelPair(internalChannel, future.channel());
            log.debug("建立连接: ClientInternal--[{}]--ClientProxy--[{}]--Responsor", internalChannel.id(), future.channel().id());
            ClientChannelGroup.removeIdleInternalChannel(internalChannel);
            ClientChannelGroup.addInternalChannel(internalChannel);
        }
        return future;
    }
}
