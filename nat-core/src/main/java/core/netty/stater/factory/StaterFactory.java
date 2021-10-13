package core.netty.stater.factory;

import core.netty.stater.client.NettyClient;
import core.netty.stater.server.NettyServer;

/**
 * @author wneck130@gmail.com
 * @Description:
 * @date 2021/9/26
 */
public interface StaterFactory {

    NettyServer getServer();

    NettyClient getClient();

}
