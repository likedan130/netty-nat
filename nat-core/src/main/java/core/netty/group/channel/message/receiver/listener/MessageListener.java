package core.netty.group.channel.message.receiver.listener;

import core.netty.group.channel.message.receiver.MessageReceiver;

/**
 * @author wneck130@gmail.com
 * @Description: 消息监听器
 * @date 2021/10/11
 */
public interface MessageListener extends MessageReceiver {

    /**
     * 根据数据帧的内容进行消息分类适配
     * @param cmd
     * @return
     */
    boolean supply(byte cmd);
}
