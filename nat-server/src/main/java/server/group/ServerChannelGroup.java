package server.group;

import core.cache.PropertiesCache;
import core.constant.FrameConstant;
import core.utils.BufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

/**
 * @Author wneck130@gmail.com
 * @Function 服务端channel组管理类
 */
@Slf4j
public class ServerChannelGroup {

   private static PropertiesCache cache = PropertiesCache.getInstance();

    /**
     * 代理程序内部连接的默认连接数
     */
   private static String INIT_NUM = "internal.channel.init.num";

    /**
     * 代理程序内部连接短缺时扩展的连接数
     */
   private static String EXPAND_NUM = "channel.pool.expand.num";

    /**
     * 代理程序内部连接最大空闲连接数，超过时触发回收
     */
   private static String MAX_IDLE = "internal.channel.max.idle.num";

    /**
     * 缓存的proxyServer对象，用来判断当前proxyServer状态
     */
   public static ChannelFuture proxyServer;

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
     * proxyChannel每次发送数据都会期待在timeout时间内收到响应，如果超时则认为连接异常，断开proxyChannel重新服务
     * ScheduledFuture为当前channel对应eventloop中的一个定义关闭任务，超时后关闭连接
     */
    private static Map<ChannelId, ScheduledFuture> futureMap = new ConcurrentHashMap<>();

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
        return idleChannel;
    }


    /**
     * 按照配置重新扩容/缩减internalGroup的数量
     */
    public static void reSizeInternalChannelNum() {
        //判断连接池剩余连接
        if(idleInternalGroup.size() < cache.getInt(INIT_NUM)){
            //发送连接池扩容命令
            ByteBuf byteBuf = Unpooled.buffer();
            byteBuf.writeByte(FrameConstant.pv);
            long serial = System.currentTimeMillis();
            byteBuf.writeLong(serial);
            byteBuf.writeShort(FrameConstant.CHANNEL_POOL_NUM_LEN + FrameConstant.VC_CODE_LEN);
            //扩容连接池数量
            byteBuf.writeByte(cache.getInt(EXPAND_NUM));
            //计算校验和
            int vc = 0;
            for (byte byteVal : BufUtil.getArray(byteBuf)) {
                vc = vc + (byteVal & 0xFF);
            }
            byteBuf.writeByte(vc);
            Channel sysClient = sysChannel.get("Sys");
            sysClient.writeAndFlush(byteBuf);
        }
        if(idleInternalGroup.size() > cache.getInt(MAX_IDLE)){
            //移除连接数量
            int num = idleInternalGroup.size() - cache.getInt(MAX_IDLE);
            //发送移除部分连接命令
            ByteBuf byteBuf = Unpooled.buffer();
            byteBuf.writeByte(FrameConstant.pv);
            long serial = System.currentTimeMillis();
            byteBuf.writeLong(serial);
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

    /**
     * 打印当前channelGroup情况
     */
    public static void printGroupState() {
        log.info("当前channel情况：\r\n"
                + "IdleInternalChannel数量：" + ServerChannelGroup.getIdleInternalGroup().size() + "\r\n"
                + "InternalChannel数量：" + ServerChannelGroup.getInternalGroup().size() + "\r\n"
                + "当前channelPair：\r\n");
        ServerChannelGroup.getchannelPair()
                .forEach((key, value) -> log.debug("[InternalChannel：" + key + ", ProxyChannel：" + value + "]\r\n"));
    }

    /**
     * 存储超时关闭的定时任务，如果已经存在任务，则cancel新任务，保持原任务不变
     * @param channelId
     * @param future
     */
    public static void addFuture(ChannelId channelId, ScheduledFuture future) {
        try {
            ScheduledFuture successOne = futureMap.putIfAbsent(channelId, future);
            if (!Objects.equals(successOne, future)) {
                future.cancel(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("超时关闭proxyChannel异常!!!", e);
        }
    }

    public static void cancelFuture(ChannelId channelId) {
        try {
            futureMap.get(channelId).cancel(true);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("关闭ScheduledFuture任务异常!!!", e);
        }
    }
}
