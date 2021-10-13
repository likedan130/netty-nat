package core.netty.group.channel.strategy;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author wneck130@gmail.com
 * @Description:
 * @date 2021/9/28
 */
public class RoundRobinForkStrategy implements ForkStrategy {

    private static AtomicInteger lastIndex = new AtomicInteger(0);

    @Override
    public Channel fork(ChannelGroup channelGroup) {
        if (channelGroup == null) {
            return null;
        }
        if (lastIndex.get() > channelGroup.size()) {
            lastIndex = new AtomicInteger(0);
        }
        List<Channel> list = new ArrayList<>(channelGroup);
        return list.get(new Random().nextInt(lastIndex.getAndIncrement()));
    }
}
