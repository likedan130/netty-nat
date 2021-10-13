package core.netty.stater.factory;

import core.netty.stater.client.NettyClient;
import core.netty.stater.server.NettyServer;

/**
 * @author wneck130@gmail.com
 * @Description:
 * @date 2021/9/26
 */
public class InternalStaterFactory implements StaterFactory {
    @Override
    public NettyServer getServer() {
        return null;
    }

    @Override
    public NettyClient getClient() {
        return null;
    }
}
