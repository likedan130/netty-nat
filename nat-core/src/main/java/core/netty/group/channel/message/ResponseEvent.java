package core.netty.group.channel.message;

import core.entity.Frame;
import lombok.Getter;
import lombok.Setter;

import java.util.EventObject;

/**
 * @author wneck130@gmail.com
 * @Description:
 * @date 2021/9/28
 */
@Setter
@Getter
public class ResponseEvent extends EventObject {

    private Frame request;

    private Frame response;

    private String channelId;

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public ResponseEvent(Object source) {
        super(source);
    }


}
