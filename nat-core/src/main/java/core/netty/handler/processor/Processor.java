package core.netty.handler.processor;

import core.netty.group.channel.message.receiver.listener.RequestListener;
import core.netty.group.channel.message.receiver.listener.ResponseListener;

/**
 * @Author wneck130@gmail.com
 * @function
 */
public interface Processor extends RequestListener, ResponseListener {

}
