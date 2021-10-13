package core.netty.group.channel.message.receiver.listener;

import core.netty.group.channel.message.ResponseEvent;

/**
 * @author wneck130@gmail.com
 * @Description: 响应类消息监听器
 * @date 2021/10/11
 */
public interface ResponseListener extends MessageListener{

    /**
     * 消息响应超时处理方法
     * @param responseEvent
     */
    default void timeout(ResponseEvent responseEvent) {}

    /**
     * 消息响应处理方法，响应内容与对应的请求内容封装在ResponseEvent中
     * @param responseEvent
     */
    default void response(ResponseEvent responseEvent) {}
}
