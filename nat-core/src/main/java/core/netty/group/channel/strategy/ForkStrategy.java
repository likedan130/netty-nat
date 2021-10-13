package core.netty.group.channel.strategy;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;

/**
 * @author wneck130@gmail.com
 * @Description:
 * @date 2021/9/28
 */
public interface ForkStrategy {

    Channel fork(ChannelGroup channelGroup);
}
