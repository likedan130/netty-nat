package server.group;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ServerChannelGroup {
   private static Logger log = LoggerFactory.getLogger(ServerChannelGroup.class);
    /**
     * 系统连接的缓存
     */
    private static Map<String, Channel> sysChannel = new ConcurrentHashMap<>();

    /**
     * channel对，将服务端与客户端的channel进行配对，便于消息转发
     * key为连接池中连接的channelId，value为proxy服务的channelId
     */
    private static Map<ChannelId, ChannelId> channelPair = new ConcurrentHashMap<>();

    /**
     * 被代理服务的channel组
     */
    private static ChannelGroup proxyGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * 内部服务的channel组
     */
    private static ChannelGroup internalGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);


    /**
     * 内部服务的channel组
     */
    private static List<Channel> idleInternalList = new ArrayList<>();

    public static void addSysChannel(Channel channel) {
        sysChannel.put("Sys", channel);
    }

    public static List<Channel> getIdleInternalList(){
        return idleInternalList;
    }

    public static Map<String, Channel> getSysChannel() {
        return sysChannel;
    }

    public static Map<ChannelId, ChannelId> getChannelPair() {
        return channelPair;
    }

    public static ChannelGroup getProxyGroup() {
        return proxyGroup;
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

    public static void removeInternalChannel(Channel channel) {
        internalGroup.remove(channel);
    }

    public static void removeIdleInternalChannel(Channel channel) {
        idleInternalList.remove(channel);
    }


    public static void addIdleInternalChannel(Channel channel) {
        idleInternalList.add(channel);
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
     * 从空闲连接池中取出一条连接并与当前proxy连接配对
     * @param channel
     * @throws Exception
     */
    public synchronized static void forkChannel(Channel channel) throws Exception{
        if (!idleInternalList.isEmpty()) {
            Channel idleChannel = idleInternalList.get(0);
            idleInternalList.remove(idleChannel);
            internalGroup.add(idleChannel);
            channelPair.put(idleChannel.id(), channel.id());
            proxyGroup.add(channel);
        } else {
            log.error("连接用尽，代理服务"+channel.id()+"配对失败!!!");
        }
    }

    /**
     * 向内部连接池转发外部应用的请求
     * @param byteBuf
     * @param channelId
     */
    public static void writeRequest(ByteBuf byteBuf, ChannelId channelId) throws Exception{
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
     * 从内部连接池将消息响应给外部的请求
     * @param byteBuf
     * @param channelId
     * @throws Exception
     */
    public static void writeResponse(ByteBuf byteBuf, ChannelId channelId) throws Exception{
        if (channelPairExist(channelId)) {
            Channel proxyChannel = getProxyByInternal(channelId);
            if (proxyChannel != null) {
                proxyChannel.writeAndFlush(byteBuf);
                return;
            }
        }
        throw new Exception("找不到对应channelPair!!!");
    }

    public static void addChannelPair(ChannelId internalChannelId, ChannelId proxyChannelId) {
        channelPair.put(internalChannelId, proxyChannelId);
    }

    public static void removeChannelPair(ChannelId internalChannelId, ChannelId proxyChannelId) {
        channelPair.remove(internalChannelId, proxyChannelId);
    }

    public static void removeChannelPair(ChannelId channelId) throws Exception{
        channelPair.remove(channelId);
        List<ChannelId> result = channelPair.entrySet().stream()
                .filter(x -> Objects.equals(x.getValue(), channelId))
                .map(y -> y.getKey())
                .collect(Collectors.toList());
        if (result.isEmpty() || result.size() != 1) {
            throw new Exception("channel移除异常!!!");
        }
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
