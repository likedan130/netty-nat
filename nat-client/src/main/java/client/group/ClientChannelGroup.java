package client.group;

import client.ProxyClient;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ClientChannelGroup {

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
        idleInternalGroup.add(channel);
    }

    /**
     * 从空闲连接池中取出一条连接并与当前proxy连接配对
     * @param channel
     * @throws Exception
     */
    public synchronized static void forkChannel(Channel channel) throws Exception{
//        while (true) {
        if (!idleInternalGroup.isEmpty()) {
            Channel idleChannel = idleInternalGroup.iterator().next();
            idleInternalGroup.remove(idleChannel);
            internalGroup.add(idleChannel);
            channelPair.put(idleChannel.id(), channel.id());
            proxyGroup.add(channel);
            System.out.println("代理服务"+channel.id()+"与内部服务"+idleChannel.id()+"配对成功");
        } else {
            System.out.println("连接用尽，代理服务"+channel.id()+"配对失败!!!");
        }
//            Thread.sleep(0);
//        }
    }

    /**
     * 根据传入的内部channel，fork出一条proxyChannel与之配对
     */
    public static void forkProxyStartChannel() throws Exception{
        //获取代理连接
        ProxyClient proxyClient = new ProxyClient();
        proxyClient.init();
        ChannelFuture channelFuture = proxyClient.start();
        Channel proxyChannel = channelFuture.channel();
        Channel channel = idleInternalGroup.iterator().next();
        //建立配对
        addChannelPair(channel, proxyChannel);
        addProxyChannel(proxyChannel);
        System.out.println("配对建立成功，"+channel.id() + ": "+proxyChannel.id());
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
                .map(x -> x.getKey())
                .collect(Collectors.toList());
        if (result.isEmpty() || result.size() != 1) {
            throw new Exception("channel匹配异常!!!");
        }
        return internalGroup.find(result.get(0));
    }
}
