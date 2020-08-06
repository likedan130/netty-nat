package client.group;

import core.cache.PropertiesCache;
import core.constant.NumberConstant;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ClientChannelGroup {
   private final static Logger log = LoggerFactory.getLogger(ClientChannelGroup.class);
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

    /**
     * 内部服务的channel组
     */
    private static List<Channel> idleInternalList = new ArrayList<>();

    /**
     * 连接代理客户端缓存关联关系，同步
     */
    public static int connectProxy = NumberConstant.ZERO;

    public static List<Object> proxyClient = new ArrayList<>();

    public static List<Channel> getIdleInternalList(){
        return idleInternalList;
    }

    public static Map<ChannelId, ChannelId> getChannelPair(){
        return channelPair;
    }

    public static ChannelGroup getInternalGroup(){
        return internalGroup;
    }

    public static void addSysChannel(Channel channel) {
        sysChannel.put("Sys", channel);
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

    public static void removeInternalChannel(Channel channel) {
        internalGroup.remove(channel);
    }

    public static void removeIdleInternalChannel(Channel channel) {
        idleInternalGroup.remove(channel);
    }

    public static void addIdleInternalChannel(Channel channel) {
        idleInternalList.add(channel);
    }


    /**
     * 根据传入的内部channel，fork出一条proxyChannel与之配对
     */
    public static synchronized Channel proxyNotExist(Channel channel) throws Exception {

        //获取代理连接
        PropertiesCache cache = (PropertiesCache)proxyClient.get(0);
        Bootstrap client = (Bootstrap)proxyClient.get(1);
        ChannelFuture channelFuture = client.connect(cache.get("proxy.client.host"),
                cache.getInt("proxy.client.port")).sync();
        Channel channel1 = channelFuture.channel();
        if(idleInternalList.contains(channel)) {
            idleInternalList.remove(channel);
        }
        if (!internalGroup.contains(channel)) {
            internalGroup.add(channel);
        }
        channelPair.remove(channel.id());
        channelPair.put(channel.id(),channel1.id());
        if(!proxyGroup.contains(channel1)){
            proxyGroup.add(channel1);
        }
        return channel1;
    }

    /**
     * 根据传入的内部channel，fork出一条proxyChannel与之配对
     */
    public static synchronized void forkProxyChannel() throws Exception {
        //获取代理连接
        PropertiesCache cache = (PropertiesCache)proxyClient.get(0);
        Bootstrap client = (Bootstrap)proxyClient.get(1);
        ChannelFuture channelFuture = client.connect(cache.get("proxy.client.host"),
                cache.getInt("proxy.client.port")).sync();
        Channel channel = channelFuture.channel();
        if (!idleInternalList.isEmpty()) {
            Channel idleChannel = idleInternalList.get(0);
            idleInternalList.remove(idleChannel);
            internalGroup.add(idleChannel);
            channelPair.put(idleChannel.id(), channel.id());
            proxyGroup.add(channel);
        } else {
            log.error("连接用尽，代理服务" + channel.id() + "配对失败!!!");
        }
    }

    public static void addChannelPair(Channel internalChannel, Channel proxyChannel) {
        channelPair.put(internalChannel.id(), proxyChannel.id());
    }

    /**
     * 向被代理程序发送实际业务消息
     * @param byteBuf
     * @param channelId
     */
    public static void writeRequest(ByteBuf byteBuf, ChannelId channelId) throws Exception{
        if (channelPairExist(channelId)) {
            Channel proxyChannel = getProxyByInternal(channelId);
            if (proxyChannel != null) {
                proxyChannel.writeAndFlush(byteBuf);
                return;
            }
        }
        throw new Exception("找不到对应channelPair!!!");
    }

    /**
     * 向内部服务器发送实际业务的响应消息
     * @param byteBuf
     * @param channelId
     * @throws Exception
     */
    public static void writeResponse(ByteBuf byteBuf, ChannelId channelId) throws Exception{
        if (channelPairExist(channelId)) {
            Channel internalChannel = getInternalByProxy(channelId);
            if (internalChannel != null) {
                internalChannel.writeAndFlush(byteBuf);
                return;
            }
        }
        throw new Exception("找不到对应channelPair!!!");
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
    public static Channel getProxyByInternal(ChannelId channelId){
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
            log.error("channel匹配异常!!!");
            throw new Exception("channel匹配异常!!!");
        }
        return internalGroup.find(result.get(0));
    }
}
