package core.netty.group.channel.message.receiver.listener;

import core.entity.Frame;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author wneck130@gmail.com
 * @Description: 请求类消息监听器
 * @date 2021/10/11
 */
public interface RequestListener extends MessageListener {

    /**
     * 消息请求处理方法
     *
     * @param ctx netty channel上下文
     * @param msg 解析成Frame结构的TCP请求数据帧
     * @throws Exception
     */
    default void request(ChannelHandlerContext ctx, Frame msg) throws Exception {
    }
}
