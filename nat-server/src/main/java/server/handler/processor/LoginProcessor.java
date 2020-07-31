package server.handler.processor;

import core.constant.FrameConstant;
import core.enums.CommandEnum;
import core.utils.BufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import server.InternalServer;
import server.ProxyServer;
import server.Server;
import server.group.ServerChannelGroup;

import java.util.Objects;

/**
 * @Author wneck130@gmail.com
 * @Function 登录命令处理器
 */
@Slf4j
public class LoginProcessor implements Processor {

    /**
     * 接入命令处理方法，收到接入指令后先校验接入密码，验证通过后启动内部服务，并向客户端发送建立连接池指令
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void process(ChannelHandlerContext ctx, ByteBuf msg) throws Exception{
        int passwordLen = msg.getByte(12) & 0xFF;
        byte[] passwordBytes = new byte[passwordLen];
        msg.getBytes(13, passwordBytes);
        String password = new String(passwordBytes, "UTF-8");
        if (Objects.equals(password, "password")) {
            //判断代理服务是否活跃
            if (!Server.proxyServer.isStarted()) {
                //认证通过，缓存当前连接，并且创建代理服务端
                ServerChannelGroup.addSysChannel(ctx.channel());
                ProxyServer proxyServer = new ProxyServer();
                proxyServer.init();
                //创建子线程启动代理服务
                new Thread(() -> {
                    try {
                        proxyServer.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
                log.info("启动代理服务（ProxyServer）成功");
            }
            //判断内部服务是否活跃
            if (!Server.internalServer.isStarted()) {
                InternalServer internalServer = new InternalServer();
                internalServer.init();
                //创建子线程启动内部服务
                new Thread(() -> {
                    try {
                        internalServer.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
                log.info("启动代理服务（InternalServer）成功");
                //响应客户端
                ByteBuf byteBuf = Unpooled.buffer();
                byteBuf.writeByte(FrameConstant.pv);
                long serial = System.currentTimeMillis();
                byteBuf.writeLong(serial);
                byteBuf.writeByte(CommandEnum.CMD_LOGIN.getCmd());
                byteBuf.writeShort(1 + 1);
                byteBuf.writeByte(FrameConstant.RESULT_SUCCESS);
                //计算校验和
                int vc = 0;
                for (byte byteVal : BufUtil.getArray(byteBuf)) {
                    vc = vc + (byteVal & 0xFF);
                }
                byteBuf.writeByte(vc);
                //给channel加个监听，如果响应成功发送建立连接池命令
                ctx.writeAndFlush(byteBuf).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        log.info("成功发送连接池命令");
                        if (future.isSuccess()) {
                            //立刻发送连接池的建立命令给客户端
                            ByteBuf byteBuf = Unpooled.buffer();
                            byteBuf.writeByte(FrameConstant.pv);
                            long serial = System.currentTimeMillis();
                            byteBuf.writeLong(serial);
                            byteBuf.writeByte(CommandEnum.CMD_CONNECTION_POOL.getCmd());
                            byteBuf.writeShort(1 + 1);
                            byteBuf.writeByte(10);//连接池数量
                            //计算校验和
                            int vc = 0;
                            for (byte byteVal : BufUtil.getArray(byteBuf)) {
                                vc = vc + (byteVal & 0xFF);
                            }
                            byteBuf.writeByte(vc);
                            ctx.writeAndFlush(byteBuf);
                        }
                    }
                });
            }
        }
    }
}
