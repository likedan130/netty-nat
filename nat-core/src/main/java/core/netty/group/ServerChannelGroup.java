package core.netty.group;

import core.entity.Tunnel;
import core.netty.group.channel.strategy.KeyBasedForkStrategy;
import core.netty.group.channel.strategy.StrategyManager;
import core.netty.group.channel.strategy.constant.ForkStrategyEnum;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * @Author wneck130@gmail.com
 * @Function 服务端channel组管理类
 */
@Slf4j
public class ServerChannelGroup {

    /**
     * 缓存的proxyServer对象，每一个proxyServer对应监听了目标端口的NioServerSocket服务
     * key为处理netty服务端连接的NioServerSocketChannel, value为对应的隧道信息
     */
   public static Map<Channel, Tunnel> proxyServers = new HashMap<>();

    /**
     * proxyServers对象的add操作方法
     * @param nioServerSocketChannel
     * @param tunnel
     */
   public static void addProxyServers(Channel nioServerSocketChannel, Tunnel tunnel) {
        proxyServers.put(nioServerSocketChannel, tunnel);
   }

    /**
     * 根据nioServerSocketChannel获取对应tunnelId，用于服务端向客户端发送预创建连接请求
     * @param nioServerSocketChannel
     * @return
     */
   public static Tunnel getTunnelByChannel(Channel nioServerSocketChannel) {
       return proxyServers.get(nioServerSocketChannel);
   }

    /**
     * 检查proxyServer是否重复开启
     * @param tunnel
     * @return
     */
   public static boolean tunnelExists(Tunnel tunnel) {
      for (Map.Entry<Channel, Tunnel> entry : proxyServers.entrySet()) {
          if (entry.getValue().getServerPort() == tunnel.getServerPort()) {
              return true;
          }
      }
      return false;
   }

    /**
     * proxyChannel每次发送数据都会期待在timeout时间内收到响应，如果超时则认为连接异常，断开proxyChannel重新服务
     * ScheduledFuture为当前channel对应eventloop中的一个定义关闭任务，超时后关闭连接
     */
    private static Map<ChannelId, ScheduledFuture> futureMap = new ConcurrentHashMap<>();

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
     * 内部服务的channel组
     */
    private static ChannelGroup internalGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public static void addInternal(Channel channel) {
        internalGroup.add(channel);
    }

    public static boolean removeInternal(Channel channel) {
        return internalGroup.remove(channel);
    }


    /**
     * channel配对关系，key为server端channelId，value为client端channelId
     * proxyServerChannel与proxyClientChannel配对
     * internalServerChannel与internalClientChannel配对
     */
    private static Map<String, String> channelPair = new ConcurrentHashMap<>();


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

    public static void addChannelPair(String serverChannelId, String clientChannelId) {
        channelPair.put(serverChannelId, clientChannelId);
    }

    /**
     * 根据serverChannelId获取配对的clientChannelId
     * @param serverChannelId
     * @return
     */
    public static String getClientByServer(String serverChannelId) {
        String clientChannelId = "";
        if (channelPair.containsKey(serverChannelId)) {
            clientChannelId = channelPair.get(serverChannelId);
        }
        return clientChannelId;
    }

    public static void removeChannelPair(Channel serverChannel, Channel clientChannel) throws Exception{
        channelPair.remove(serverChannel, clientChannel);
    }

    /**
     * 判断当前channel是否已经建立channelPair，没有建立channelPair的channel无法进行转发
     * @param channel
     * @return
     */
    public static boolean channelPairExist(Channel channel) {
        if (channelPair.isEmpty()) {
            return Boolean.FALSE;
        }
        if (channelPair.containsKey(channel)) {
            return Boolean.TRUE;
        }
        if (channelPair.containsValue(channel)) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }



    /**
     * 打印当前channelGroup情况
     */
    public static void printGroupState() {
        log.info("当前channel情况：\r\n"
                + "ProxyChannel数量：" + ServerChannelGroup.proxyGroup.size() + "\r\n"
                + "InternalChannel数量：" + ServerChannelGroup.internalGroup.size() + "\r\n"
                + "当前channelPair：\r\n");
        ServerChannelGroup.channelPair
                .forEach((key, value) -> log.debug("[ServerChannel：" + key + ", ClientChannel：" + value + "]\r\n"));
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
