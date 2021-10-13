package core.netty.group.channel.strategy;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;

import java.util.Comparator;

/**
 * @author wneck130@gmail.com
 * @Description:
 * @date 2021/9/28
 */
public class MinLoadForkStrategy implements ForkStrategy{
    @Override
    public Channel fork(ChannelGroup channelGroup) {
        if (channelGroup == null) {
            return null;
        }
        return channelGroup.stream().min(Comparator.comparingLong(Channel::bytesBeforeWritable)).get();
    }
}
