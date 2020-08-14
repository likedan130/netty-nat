package client.group;

import client.InternalClient;
import core.cache.PropertiesCache;
import core.constant.FrameConstant;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
//
//    /**
//     * 连接代理客户端缓存关联关系，同步
//     */
//    public static int connectProxy = 0;

    /**
     *按顺序存储proxyClient启动信息
     *index[0] PropertiesCache配置文件参数
     * index[1] Bootstrap — 启动类
     */
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

    public static void removeInternalChannel(Channel channel) throws Exception{
        internalGroup.remove(channel);
    }

    public static void removeIdleInternalChannel(Channel channel) throws Exception{
        idleInternalGroup.remove(channel);
    }

    public static void addIdleInternalChannel(Channel channel) {
        idleInternalList.add(channel);
    }

    /**
     * 移除部分连接
     */
    public static void removeInternalChannel(ByteBuf msg) throws Exception{
        //移除数量
//        int connectionNum = msg.getByte(NumberConstant.TWELVE) & 0xFF;
//        for (int i = 0; i < connectionNum; i++) {
//            idleInternalList.get(i).close();
//        }
    }

    /**
     * 连接池扩容
     */
    public static void connectionPoolExpansion(ByteBuf msg) throws Exception{
        int connectionNum = msg.getByte(FrameConstant.FRAME_DTAT_FIRST_BYTE_INDEX) & 0xFF;
        //启动内部客户端连接池
        InternalClient internalClient = new InternalClient();
        internalClient.init();
        internalClient.start(connectionNum);
    }

    /**
     * 根据传入的内部channel，fork出一条proxyChannel与之配对
     */
    public static synchronized void forkProxyChannel(ChannelId channelId) throws Exception {
        //获取代理连接
        if (!idleInternalList.isEmpty()) {
            List<Channel> idleChannel = idleInternalList.stream().filter(e -> Objects.equals(e.id().toString(),channelId.toString())).collect(Collectors.toList());
            idleInternalList.remove(idleChannel.get(0));
            internalGroup.add(idleChannel.get(0));
            PropertiesCache cache = (PropertiesCache)proxyClient.get(0);
            Bootstrap client = (Bootstrap)proxyClient.get(1);
            ChannelFuture channelFuture = client.connect(cache.get("proxy.client.host"),
                    cache.getInt("proxy.client.port")).sync();
            Channel channel = channelFuture.channel();
            channelPair.put(idleChannel.get(0).id(), channel.id());
            proxyGroup.add(channel);
        } else {
            log.error("连接用尽，代理服务" + channelId + "配对失败!!!");
        }
    }

    public static void addChannelPair(Channel internalChannel, Channel proxyChannel) {
        channelPair.put(internalChannel.id(), proxyChannel.id());
    }

    public static void removeChannelPair(ChannelId internalChannelId, ChannelId proxyChannelId) throws Exception{
        channelPair.remove(internalChannelId, proxyChannelId);
    }

    /**
     * 释放占用的连接池连接
     * @param channel
     */
    public static void releaseInternalChannel(Channel channel) {
        internalGroup.remove(channel);
        idleInternalList.add(channel);
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
            log.error("channel匹配异常!!!");
            throw new Exception("channel匹配异常!!!");
        }
        return internalGroup.find(result.get(0));
    }
}
