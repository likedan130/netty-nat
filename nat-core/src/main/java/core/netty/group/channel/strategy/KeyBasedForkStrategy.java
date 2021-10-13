package core.netty.group.channel.strategy;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wneck130@gmail.com
 * @Description:
 * @date 2021/9/28
 */
@Setter
public class KeyBasedForkStrategy implements ForkStrategy{

    private String key;

    @Override
    public Channel fork(ChannelGroup channelGroup) {
        if (channelGroup == null || channelGroup.size() == 0) {
            return null;
        }
        List<Channel> list = new ArrayList<>(channelGroup);
        return list.get(Math.abs(key.hashCode()%channelGroup.size()));
    }
}
