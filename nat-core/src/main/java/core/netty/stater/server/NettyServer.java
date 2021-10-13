package core.netty.stater.server;

/**
 * @author wneck130@gmail.com
 * @Description:
 * @date 2021/9/26
 */
public interface NettyServer {

    void init();

    void start(int port);

    boolean isHeathy();

    boolean isRunning();
}
