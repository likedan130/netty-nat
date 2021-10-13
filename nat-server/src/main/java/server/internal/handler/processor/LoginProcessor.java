package server.internal.handler.processor;

import core.constant.FrameConstant;
import core.entity.Frame;
import core.entity.Tunnel;
import core.netty.group.ServerChannelGroup;
import core.netty.handler.processor.Processor;
import core.properties.cache.PropertiesCache;
import core.utils.BufUtil;
import core.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import server.internal.handler.processor.constant.ProcessorEnum;
import server.proxy.ProxyNettyServer;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @Author wneck130@gmail.com
 * @Function 接入命令处理器（cmd:0x01），命令由internalClient发起请求，internalServer回复响应
 */
@Slf4j
public class LoginProcessor implements Processor {

    private final String DATA_KEY_PASSWORD = "password";

    private final String DATA_KEY_TUNNELS = "tunnels";

    private final String DEFAULT_PASSWORD = PropertiesCache.getInstance().get("password");

    /**
     * login数据帧处理
     * @param in netty获取的TCP数据流
     * @return
     * @throws Exception
     */
    @Override
    public Frame assemble(ByteBuf in) throws Exception {
        try {
            //解析协议公共部分
            Frame frame = new Frame().quickHead(in);
            //解析协议data部分
            byte passwordLen = in.readByte();
            byte[] passwordBytes = new byte[passwordLen & 0xFF];
            in.readBytes(passwordBytes);
            String password = new String(passwordBytes, StandardCharsets.UTF_8);
            List<Tunnel> tunnelList = new ArrayList<>();
            while (in.readableBytes() > 0) {
                int serverPort = in.readUnsignedShort();
                int clientHostSegment1 = in.readByte() & 0xFF;
                int clientHostSegment2 = in.readByte() & 0xFF;
                int clientHostSegment3 = in.readByte() & 0xFF;
                int clientHostSegment4 = in.readByte() & 0xFF;
                String clientHost = new StringBuilder().append(clientHostSegment1)
                        .append(".")
                        .append(clientHostSegment2)
                        .append(".")
                        .append(clientHostSegment3)
                        .append(".")
                        .append(clientHostSegment4).toString();
                int clientPort = in.readUnsignedShort();
                Tunnel tunnel = new Tunnel();
                tunnel.setServerPort(serverPort);
                tunnel.setClientHost(clientHost);
                tunnel.setClientPort(clientPort);
                tunnelList.add(tunnel);
            }
            LinkedHashMap<String, Object> dataMap = new LinkedHashMap<>(2);
            dataMap.put(DATA_KEY_PASSWORD, password);
            dataMap.put(DATA_KEY_TUNNELS, tunnelList);
            frame.setData(dataMap);
            return frame;
        } catch (Exception e) {
            log.error("无法解析的消息: " + ByteUtil.toHexString(BufUtil.getArray(in)));
            throw new UnknownFormatConversionException("无法解析的消息!!!");
        }
    }

    /**
     * login消息处理业务
     * @param ctx netty channel上下文
     * @param msg 解析成Frame结构的TCP请求数据帧
     * @throws Exception
     */
    @Override
    public void request(ChannelHandlerContext ctx, Frame msg) throws Exception {
        Map<String, Object> requestData = msg.getData();
        //响应消息
        Frame response = new Frame();
        response.setPv(FrameConstant.RES_PV);
        response.setSerial(msg.getSerial());
        response.setRes(msg.getRes());
        response.setReq(msg.getReq());
        response.setCmd(ProcessorEnum.LOGIN.getCmd());
        LinkedHashMap<String, Object> responseData = new LinkedHashMap<>(1);
        responseData.put("result", FrameConstant.RESULT_SUCCESS);
        response.setData(responseData);
        String password = (String) requestData.get(DATA_KEY_PASSWORD);
        //密码校验失败，直接返回错误信息并关闭channel
        if (!Objects.equals(DEFAULT_PASSWORD, password)) {
            responseData.put("result", FrameConstant.RESULT_FAIL);
            ctx.writeAndFlush(response);
            ctx.close();
            return;
        }
        List<Tunnel> tunnels = (List<Tunnel>) requestData.get(DATA_KEY_TUNNELS);
        tunnels.forEach(tunnel -> {
                if (ServerChannelGroup.tunnelExists(tunnel)) {
                    return;
                }
                new Thread(() -> {
                ProxyNettyServer proxyNettyServer = new ProxyNettyServer();
                proxyNettyServer.start(tunnel.getServerPort());
                proxyNettyServer.getNioServerFuture().addListener((future) -> {
                    if (future.isSuccess()) {
                        //端口监听创建成功后，缓存proxyNettyServer和tunnel信息
                        ServerChannelGroup.addProxyServers(proxyNettyServer.getNioServerFuture().channel(),tunnel);
                        log.debug("成功创建tunnel：[serverPort:{}], [ClientHost{}], [ClientPort:{}]",
                                tunnel.getServerPort(), tunnel.getClientHost(), tunnel.getClientPort());
                    }
                });
            }).start();
        });
        ctx.writeAndFlush(response);
    }

    /**
     * processor对应的命令字适配
     * @param cmd
     * @return
     */
    @Override
    public boolean supply(byte cmd) {
        return Objects.equals(ProcessorEnum.LOGIN.getCmd(), cmd);
    }
}
