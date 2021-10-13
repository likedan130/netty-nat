package core.entity;

import core.constant.FrameConstant;
import core.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class Frame {

    private byte pv = FrameConstant.REQ_PV;

    private long serial = System.currentTimeMillis();

    private String req;

    private String res;

    private byte cmd;

    private int len;

    private LinkedHashMap<String, Object> data = new LinkedHashMap<>();

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("cmd:"+ ByteUtil.toHexString(cmd) + "\n");
        sb.append("serial:"+ serial + "\n");
        sb.append("req:"+ req + "\n");
        sb.append("res:"+ res + "\n");
        if (!data.isEmpty()) {
            data.forEach((key, value) -> {
                if (value instanceof Byte) {
                    sb.append(key + ByteUtil.toHexString((byte)value) + "\n");
                } else if (value instanceof Byte[]) {
                    sb.append(key + ByteUtil.toHexString((byte[])value) + "\n");
                } else {
                    sb.append(key + value.toString() + "\n");
                }
            });
        }
        return sb.toString();
    }

    /**
     * 快速解析公共协议部分的内容
     * @param in
     */
    public Frame quickHead(ByteBuf in) {
        //解析协议公共部分
        byte pv = in.readByte();
        long serial = in.readLong();
        byte[] reqBytes = new byte[8];
        in.readBytes(reqBytes);
        String req = new String(reqBytes, StandardCharsets.UTF_8);
        byte[] resBytes = new byte[8];
        in.readBytes(resBytes);
        String res = new String(resBytes, StandardCharsets.UTF_8);
        byte cmd = in.readByte();
        int len = in.readInt();
        this.setPv(pv);
        this.setSerial(serial);
        this.setReq(req);
        this.setRes(res);
        this.setCmd(cmd);
        this.setLen(len);
        return this;
    }
}
