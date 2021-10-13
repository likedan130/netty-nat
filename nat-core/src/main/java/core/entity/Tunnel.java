package core.entity;

import lombok.Data;

/**
 * @author wneck130@gmail.com
 * @Description: 通信隧道，从服务端到客户端的全链路形容，以serverPort作为唯一标识
 * @date 2021/9/24
 */
@Data
public class Tunnel {

    /**
     * 服务端端口号
     */
    private int serverPort;

    /**
     * 客户端被代理服务IP
     */
    private String clientHost;

    /**
     * 客户端被代理服务端口号
     */
    private int clientPort;
}
