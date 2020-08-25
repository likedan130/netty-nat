package client.group;

import client.Client;
import client.InternalClient;
import core.cache.PropertiesCache;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ClientChannelGroup {
   private final static Logger log = LoggerFactory.getLogger(ClientChannelGroup.class);

   private static PropertiesCache cache = PropertiesCache.getInstance();
    /**
     * 系统连接的缓存
     */
    private static Map<String, Channel> sysChannel = new ConcurrentHashMap<>();

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

    /**
     *按顺序存储proxyClient启动信息
     *index[0] PropertiesCache配置文件参数
     * index[1] Bootstrap — 启动类
     */
    public static List<Object> proxyClient = new ArrayList<>();

    public static int getIdleInternalGroupSize() {
        return idleInternalGroup.size();
    }

    public static Map<ChannelId, ChannelId> getChannelPair(){
        return channelPair;
    }

    public static ChannelGroup getInternalGroup(){
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

    public static void removeInternalChannel(Channel channel) throws Exception{
        internalGroup.remove(channel);
    }

    public static void removeIdleInternalChannel(Channel channel) throws Exception{
        idleInternalGroup.remove(channel);
//        //所有连接都断开了，则判断为丢失与服务器的连接，进行重连
//        if (idleInternalGroup.size() == 0) {
//            InternalClient internalClient = new InternalClient();
//            Client.threadPoolExecutor.execute(() -> {
//                internalClient.reConnect();
//            });
//        }
        //检查空闲连接是否小于最小空闲连接数
        if (idleInternalGroup.size() < cache.getInt("internal.channel.min.idle.num")) {
            InternalClient internalClient = new InternalClient();
            Client.threadPoolExecutor.execute(() -> {
                if (!InternalClient.isChanging()) {
                    internalClient.connect(cache.getInt("internal.channel.max.idle.num") / 2);
                }
            });
        }
    }

    public static void addIdleInternalChannel(Channel channel) {
        idleInternalGroup.add(channel);
        //检查空闲连接是否大于最大空闲连接数，成立则关闭当前连接
        if (idleInternalGroup.size() > cache.getInt("internal.channel.max.idle.num")) {
            channel.close();
        }
    }

    public static void addChannelPair(Channel internalChannel, Channel proxyChannel) {
        channelPair.put(internalChannel.id(), proxyChannel.id());
    }

    public static void removeChannelPair(ChannelId internalChannelId, ChannelId proxyChannelId) throws Exception{
        channelPair.remove(internalChannelId, proxyChannelId);
    }

    public static void removeChannelPair(ChannelId internalChannelId) throws Exception{
        channelPair.remove(internalChannelId);
    }

    /**
     * 释放占用的连接池连接
     * @param channel
     */
    public static void releaseInternalChannel(Channel channel) {
        internalGroup.remove(channel);
        addIdleInternalChannel(channel);
    }

    /**
     * 判断当前channel是否已经建立channelPair，没有建立channelPair的channel无法进行转发
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
     * @param channelId
     * @return
     */
    public static Channel getProxyByInternal(ChannelId channelId) throws Exception{
        ChannelId proxyChannelId = channelPair.get(channelId);
        return proxyGroup.find(proxyChannelId);
    }

    /**
     * 根据代理channleId获取内部channel
     * @param channelId
     * @return
     */
    public static Channel getInternalByProxy(ChannelId channelId) throws Exception{
        List<ChannelId> result = channelPair.entrySet().stream()
                .filter(e -> Objects.equals(e.getValue(), channelId))
                .map((x) -> x.getKey())
                .collect(Collectors.toList());
        if (result.isEmpty() || result.size() != 1) {
            log.error("找不到proxyChannel"+channelId+"!!!");
            return null;
        }
        return internalGroup.find(result.get(0));
    }

    public static void printGroupState() {
        LocalDateTime localDateTime = LocalDateTime.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
//        System.out.println("[DEBUG] "+dtf.format(localDateTime)+" -  当前channel情况：");
//        System.out.println("[DEBUG] "+dtf.format(localDateTime)+" -  IdleInternalChannel数量：" + ClientChannelGroup.getIdleInternalGroup().size());
//        System.out.println("[DEBUG] "+dtf.format(localDateTime)+" -  InternalChannel数量：" + ClientChannelGroup.getInternalGroup().size());
        log.debug("当前channel情况：");
        log.debug("IdleInternalChannel数量：" + ClientChannelGroup.getIdleInternalGroup().size());
        log.debug("InternalChannel数量：" + ClientChannelGroup.getInternalGroup().size());
        log.debug("当前channelPair：");
        ClientChannelGroup.getChannelPair().entrySet().stream().forEach((entry) -> {
            log.debug("[InternalChannel：" + entry.getKey()+", ProxyChannel："+entry.getValue()+"]");
//            System.out.println("[DEBUG] "+dtf.format(localDateTime)+" -  [InternalChannel：" + entry.getKey()+", ProxyChannel："+entry.getValue()+"]");
        });
    }
}
