package client.internal.handler.processor;

/**
 * @Author wneck130@gmail.com
 * @function internal连接回收命令处理器，server端检测到连接断开后发送给客户端，确保无用tcp连接被回收
 */
//@Slf4j
//public class ChannelRecycleProcessor implements Processor {
//
//    @Override
//    public Frame assemble(ByteBuf in) throws Exception {
//        return null;
//    }
//
//    @Override
//    public void request(ChannelHandlerContext ctx, Frame msg) throws Exception {
//        Channel internalChannel = ctx.channel();
//        log.debug("InternalChannel："+ internalChannel.id() + " 回收连接!!!");
//        if (ClientChannelGroup.channelPairExist(internalChannel.id())) {
//            ClientChannelGroup.printGroupState();
//            Channel proxyChannel = ClientChannelGroup.getProxyByInternal(internalChannel.id());
//            ClientChannelGroup.removeProxyChannel(proxyChannel);
//            if (internalChannel ==  null) {
//                log.error("与internalChannel："+internalChannel.id()+"配对的proxyChannel为null");
//                return;
//            }
//            ClientChannelGroup.removeChannelPair(internalChannel.id(), ctx.channel().id());
//            ClientChannelGroup.releaseInternalChannel(internalChannel);
//        } else {
//            log.error("internalChannel："+ internalChannel.id() + " 未找到配对关系!!!");
//            ClientChannelGroup.printGroupState();
//        }
//    }
//}
