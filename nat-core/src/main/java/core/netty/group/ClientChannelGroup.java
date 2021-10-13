package core.netty.group;

import core.entity.Tunnel;
import core.properties.cache.PropertiesCache;
import core.netty.group.channel.strategy.KeyBasedForkStrategy;
import core.netty.group.channel.strategy.StrategyManager;
import core.netty.group.channel.strategy.constant.ForkStrategyEnum;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author wneck130@gmail.com
 * @function 客户端连接组，管理所有客户端的TCP连接
 */
@Slf4j
public class ClientChannelGroup {

    /**
     * 隧道信息，key为serverPort，value为tunnel完整信息
     */
    private static Map<Integer, Tunnel> tunnels = new ConcurrentHashMap<>();

    /**
     * 添加tunnel
     * @param serverPort
     * @param tunnel
     */
    public static void addTunnel(Integer serverPort, Tunnel tunnel) {
        tunnels.put(serverPort, tunnel);
    }

    /**
     * 查找tunnel
     * @param serverPort
     * @return
     */
    public static Tunnel getTunnel(Integer serverPort) {
        return tunnels.get(serverPort);
    }

    /**
     * channel配对关系，key为server端channelId，value为client端channelId
     * proxyServerChannel与proxyClientChannel配对
     * internalServerChannel与internalClientChannel配对
     */
    private static Map<String, String> channelPair = new ConcurrentHashMap<>();

    public static void addChannelPair(String serverChannelId, String clientChannelId) {
        channelPair.put(serverChannelId, clientChannelId);
    }

    public static void removeChannelPair(String serverChannelId, String clientChannelId) throws Exception{
        channelPair.remove(serverChannelId, clientChannelId);
    }

    public static String getServerChannel(String clientChannelId) {
        String serverChannelId = "";
        if (!channelPair.containsValue(clientChannelId)) {
            return serverChannelId;
        }
        for(Map.Entry<String, String> entry : channelPair.entrySet()) {
            if (Objects.equals(entry.getValue(), clientChannelId)) {
                serverChannelId = entry.getKey();
            }
        }
        return serverChannelId;
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
     * 从连接池中取出一条连接
     * @param forkStrategyEnum 获取连接的策略
     * @throws Exception
     */
    public static synchronized Channel forkChannel(ForkStrategyEnum forkStrategyEnum) throws Exception{
        return StrategyManager.getInstance(forkStrategyEnum.getClazz()).fork(internalGroup);
    }

    /**
     * 从连接池中取出一条连接
     * @param basedKey 基于key值的channel获取策略，key值一般可以直接使用channelId，当使用此策略时，同一业务的数据具有顺序性
     * @throws Exception
     */
    public static synchronized Channel forkChannel(String basedKey) throws Exception{
        KeyBasedForkStrategy keyBasedForkStrategy = (KeyBasedForkStrategy)StrategyManager
                .getInstance(ForkStrategyEnum.KEY.getClazz());
        keyBasedForkStrategy.setKey(basedKey);
        return keyBasedForkStrategy.fork(internalGroup);
    }

    /**
     * 被代理服务的channel组
     */
    private static Map<String, Channel> proxyGroup = new ConcurrentHashMap<>();

    /**
     * 代理程序添加channel
     * @param channel
     */
    public static void addProxy(Channel channel) {
        String channelId = channel.id().asShortText();
        proxyGroup.put(channelId, channel);
    }

    /**
     * 代理程序查找channel
     * @param channelId
     * @return
     */
    public static Channel findProxy(String channelId) {
        return proxyGroup.get(channelId);
    }

    /**
     * 代理程序删除channel
     * @param channel
     * @return
     */
    public static boolean removeProxy(Channel channel) {
        String channelId = channel.id().asShortText();
        return proxyGroup.remove(channelId, channel);
    }

    /**
     * 系统内部连接池的channel组
     */
    public static ChannelGroup internalGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public static void addInternal(Channel channel) {
        internalGroup.add(channel);
    }

    public static boolean removeInternal(Channel channel) {
        return internalGroup.remove(channel);
    }



    public static void printGroupState() {
        log.debug("当前channel情况：");
        log.debug("proxyChannel数量：" + ClientChannelGroup.proxyGroup.size());
        log.debug("InternalChannel数量：" + ClientChannelGroup.internalGroup.size());
        log.debug("当前channelPair：");
        ClientChannelGroup.channelPair.entrySet().stream().forEach((entry) -> {
            log.debug("[InternalChannel：" + entry.getKey() + ", ProxyChannel：" + entry.getValue() + "]");
        });
    }
}
