package server.group;

import core.cache.PropertiesCache;
import core.constant.FrameConstant;
import core.enums.CommandEnum;
import core.utils.BufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.Server;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ServerChannelGroup {
   private static Logger log = LoggerFactory.getLogger(ServerChannelGroup.class);

   private static PropertiesCache cache = PropertiesCache.getInstance();

   private static boolean proxyServerStarted = false;

    private static boolean proxyServerHttpStarted = false;

    /**
     * 系统连接的缓存
     */
    private static Map<String, Channel> sysChannel = new ConcurrentHashMap<>();

    /**
     * channel对，将ProxyServer与InternalServer的channel进行配对，便于消息转发
     * key为InternalServer中channel的channelId，value为ProxyServer中channel的channelId
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
    private static ChannelGroup idleInternalGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

//    public static void addSysChannel(Channel channel) {
//        sysChannel.putIfAbsent("Sys", channel);
//        idleInternalGroup.remove(channel);
//    }

//    public static boolean sysChannelIsEmpty() {
//        if (sysChannel == null) {
//            return Boolean.TRUE;
//        }
//        return sysChannel.size() < 1;
//    }

//    public static boolean isSysChannel(Channel channel) {
//        return sysChannel.get("Sys").equals(channel);
//    }


    public static Map<ChannelId, ChannelId> getChannelPair() {
        return channelPair;
    }

    public static void setChannelPair(Map<ChannelId, ChannelId> channelPair) {
        ServerChannelGroup.channelPair = channelPair;
    }

    public static void setInternalGroup(ChannelGroup internalGroup) {
        ServerChannelGroup.internalGroup = internalGroup;
    }

    public static ChannelGroup getIdleInternalGroup() {
        return idleInternalGroup;
    }

    public static void setIdleInternalGroup(ChannelGroup idleInternalGroup) {
        ServerChannelGroup.idleInternalGroup = idleInternalGroup;
    }

    public static void setProxyServerStarted(Boolean isStarted) {
        proxyServerStarted = isStarted;
    }

    public static void setProxyServerHttpStarted(Boolean isStarted) {
        proxyServerHttpStarted = isStarted;
    }

    public static boolean getProxyServerStarted() {
        return proxyServerStarted;
    }

    public static boolean getProxyServerHttpStarted() {
        return proxyServerHttpStarted;
    }

    public static Map<String, Channel> getSysChannel() {
        return sysChannel;
    }

    public static Map<ChannelId, ChannelId> getchannelPair() {
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

    public static void removeProxyChannel(Channel channel) throws Exception{
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
        idleInternalGroup.add(channel);
    }

    public static boolean idleInternalGroupIsEmpty() {
        if (idleInternalGroup == null) {
            return Boolean.TRUE;
        }
        return idleInternalGroup.size() < 1;
    }

    /**
     * 释放占用的连接池连接
     * @param channel
     */
    public static void releaseInternalChannel(Channel channel){
        internalGroup.remove(channel);
        idleInternalGroup.add(channel);
    }

    /**
     * 从空闲连接池中取出一条连接并与当前proxy连接配对
     * @param proxyChannel
     * @throws Exception
     */
    public synchronized static Channel forkChannel(Channel proxyChannel) throws Exception{
        Channel idleChannel = idleInternalGroup.iterator().next();
        idleInternalGroup.remove(idleChannel);
        internalGroup.add(idleChannel);
        channelPair.put(idleChannel.id(), proxyChannel.id());
        proxyGroup.add(proxyChannel);
//        ReSizeInternalChannelNum();
        return idleChannel;
    }


    /**
     * 按照配置重新扩容/缩减internalGroup的数量
     */
    public static void ReSizeInternalChannelNum() {
        //判断连接池剩余连接
        if(idleInternalGroup.size() < cache.getInt("internal.channel.init.num")){
            //发送连接池扩容命令
            ByteBuf byteBuf = Unpooled.buffer();
            byteBuf.writeByte(FrameConstant.pv);
            long serial = System.currentTimeMillis();
            byteBuf.writeLong(serial);
//            byteBuf.writeByte(CommandEnum.CMD_CONNECTION_POOL_EXPANSION.getCmd());
            byteBuf.writeShort(FrameConstant.CHANNEL_POOL_NUM_LEN + FrameConstant.VC_CODE_LEN);
            //扩容连接池数量
            byteBuf.writeByte(cache.getInt("channel.pool.expand.num"));
            //计算校验和
            int vc = 0;
            for (byte byteVal : BufUtil.getArray(byteBuf)) {
                vc = vc + (byteVal & 0xFF);
            }
            byteBuf.writeByte(vc);
            Channel sysClient = sysChannel.get("Sys");
            sysClient.writeAndFlush(byteBuf);
        }
        if(idleInternalGroup.size() > cache.getInt("internal.channel.max.idle.num")){
            //移除连接数量
            int num = idleInternalGroup.size() - cache.getInt("internal.channel.max.idle.num");
            //发送移除部分连接命令
            ByteBuf byteBuf = Unpooled.buffer();
            byteBuf.writeByte(FrameConstant.pv);
            long serial = System.currentTimeMillis();
            byteBuf.writeLong(serial);
//            byteBuf.writeByte(CommandEnum.CMD_REMOVE_THE_CONNECTION.getCmd());
            byteBuf.writeShort(FrameConstant.CHANNEL_POOL_NUM_LEN + FrameConstant.VC_CODE_LEN);
            //连接池移除数量
            byteBuf.writeByte(num);
            //计算校验和
            int vc = 0;
            for (byte byteVal : BufUtil.getArray(byteBuf)) {
                vc = vc + (byteVal & 0xFF);
            }
            byteBuf.writeByte(vc);
            Channel sysClient = sysChannel.get("Sys");
            sysClient.writeAndFlush(byteBuf);
        }
    }


//    /**
//     * 向内部连接池转发外部应用的请求
//     * @param byteBuf
//     * @param channelId
//     */
//    public static void writeRequest(ByteBuf byteBuf, ChannelId channelId) throws Exception{
//        if (channelPairExist(channelId)) {
//            Channel internalChannel = getInternalByProxy(channelId);
//            if (internalChannel != null) {
//                internalChannel.writeAndFlush(byteBuf);
//                return;
//            }
//        }
//        throw new Exception("找不到对应channelPair!!!");
//    }
//
//    /**
//     * 从内部连接池将消息响应给外部的请求
//     * @param byteBuf
//     * @param channelId
//     * @throws Exception
//     */
//    public static void writeResponse(ByteBuf byteBuf, ChannelId channelId) throws Exception{
//        if (channelPairExist(channelId)) {
//            Channel proxyChannel = getProxyByInternal(channelId);
//            if (proxyChannel != null) {
//                proxyChannel.writeAndFlush(byteBuf);
//                return;
//            }
//        }
//        throw new Exception("找不到对应channelPair!!!");
//    }

    public static void addChannelPair(ChannelId internalChannelId, ChannelId proxyChannelId) {
        channelPair.put(internalChannelId, proxyChannelId);
    }

    public static void removeChannelPair(ChannelId internalChannelId, ChannelId proxyChannelId) throws Exception{
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
            return null;
        }
        return internalGroup.find(result.get(0));
    }

    public static void printGroupState() {
        LocalDateTime localDateTime = LocalDateTime.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
//        System.out.println("[DEBUG] "+dtf.format(localDateTime)+" -  当前channel情况：");
//        System.out.println("[DEBUG] "+dtf.format(localDateTime)+" -  IdleInternalChannel数量：" + ServerChannelGroup.getIdleInternalGroup().size());
//        System.out.println("[DEBUG] "+dtf.format(localDateTime)+" -  InternalChannel数量：" + ServerChannelGroup.getInternalGroup().size());
        log.info("当前channel情况：");
        log.info("IdleInternalChannel数量：" + ServerChannelGroup.getIdleInternalGroup().size());
        log.info("InternalChannel数量：" + ServerChannelGroup.getInternalGroup().size());
        log.info("当前channelPair：");
        ServerChannelGroup.getchannelPair().entrySet().stream().forEach((entry) -> {
//            System.out.println("[DEBUG] "+dtf.format(localDateTime)+" -  [InternalChannel：" + entry.getKey()+", ProxyChannel："+entry.getValue()+"]");
            log.debug("[InternalChannel：" + entry.getKey()+", ProxyChannel："+entry.getValue()+"]");
        });
    }
}
