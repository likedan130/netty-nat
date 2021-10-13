package core.netty.group.channel.strategy;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author wneck130@gmail.com
 * @Description:
 * @date 2021/9/28
 */
public class RandomForkStrategy implements ForkStrategy {

    @Override
    public Channel fork(ChannelGroup channelGroup) {
        if (channelGroup == null) {
            return null;
        }
        List<Channel> list = new ArrayList<>(channelGroup);
        return list.get(new Random().nextInt(list.size()));
    }
}
