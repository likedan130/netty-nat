package core.netty.group.channel.message;

import core.entity.Frame;
import core.netty.group.channel.message.receiver.listener.ResponseListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author wneck130@gmail.com
 * @Description:
 * @date 2021/9/28
 */
public class MessageContext {

    public static ScheduledThreadPoolExecutor scheduled = new ScheduledThreadPoolExecutor(1);

    private MessageContext(){
        //对缓存的所有消息进行超时判断，间隔1秒
        scheduled.scheduleAtFixedRate(() ->
            historyMessage.entrySet().stream().forEach(stringListEntry -> {
                String channelId = stringListEntry.getKey();
                stringListEntry.getValue().stream()
                        .filter(request -> request.getSerial() < System.currentTimeMillis() - 30000L)
                        .forEach(timeoutRequest -> {
                            ResponseEvent responseEvent = new ResponseEvent(this);
                            responseEvent.setRequest(timeoutRequest);
                            responseEvent.setChannelId(channelId);
                            this.notifyTimeout(responseEvent);
                        });
            }), 1, 1, TimeUnit.SECONDS);
    }

    public static MessageContext getInstance() {
        return Holder.instance;
    }

    private static class Holder {
        private static MessageContext instance = new MessageContext();

    }

    private List<ResponseListener> listeners = new ArrayList<>();

    /**
     * 发送历史消息合集，当发送消息后添加了响应监听器，则缓存当前发送的消息
     * Key为proxyServer的channelId，value为消息frame的serial字段值
     */
    private static Map<String, List<Frame>> historyMessage = new ConcurrentHashMap<>();

    public static void addHistoryFrame(String channelId, Frame frame) {
        if (historyMessage.containsKey(channelId)) {
            List<Frame> frames = historyMessage.get(channelId);
            if (frames == null) {
                frames = new ArrayList<>();
            }
            frames.add(frame);
            historyMessage.put(channelId, frames);
        } else {
            List<Frame> frames = new ArrayList<>();
            frames.add(frame);
            historyMessage.put(channelId, frames);
        }
    }

    /**
     * 清除消息发送记录
     * @param channelId
     * @param frame
     * @return
     */
    private void removeHistory(String channelId, Frame frame) {
        if (!historyMessage.containsKey(channelId)) {
            return;
        }
        List<Frame> frames = historyMessage.get(channelId);
        if (frames == null || frames.isEmpty()) {
            return;
        }
        frames.remove(frame);
        historyMessage.put(channelId, frames);
    }

    public static Frame getHistoryFrame(String channelId, Long serial) {
        List<Frame> historyRequest = historyMessage.get(channelId);
        if (historyRequest == null) {
            return null;
        }
        return historyRequest.stream().filter(frame -> Objects.equals(frame.getSerial(), serial)).findFirst().get();
    }

    public void addResponseListener(ResponseListener responseListener) {
        if (!listeners.contains(responseListener)) {
            listeners.add(responseListener);
        }
    }

    /**
     * 响应超时消息
     */
    public void notifyTimeout(ResponseEvent responseEvent) {
        listeners.forEach(responseListener -> responseListener.timeout(responseEvent));
        removeHistory(responseEvent.getChannelId(), responseEvent.getRequest());
    }

    /**
     * 响应事件通知
     * @param responseEvent
     */
    public void notifyResponse(ResponseEvent responseEvent) {
        byte cmd = responseEvent.getRequest().getCmd();
        listeners.stream().filter(responseListener -> responseListener.supply(cmd))
                 .forEach(responseListener -> responseListener.response(responseEvent));
        removeHistory(responseEvent.getChannelId(), responseEvent.getRequest());
    }



}
