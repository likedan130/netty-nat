package core.netty.group.channel.message.receiver;

import core.entity.Frame;
import io.netty.buffer.ByteBuf;

/**
 * @author wneck130@gmail.com
 * @Description: 消息接收器
 * @date 2021/10/11
 */
public interface MessageReceiver {

    /**
     * 接受消息时针对自定义协议解析数据帧，将协议中内容转化成Frame对象为下游提供数据输入
     * @param in
     * @return
     * @throws Exception
     */
    Frame assemble(ByteBuf in) throws Exception;
}
