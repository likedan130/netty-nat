package core.entity;

import core.constant.FrameConstant;
import core.utils.ByteUtil;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class Frame {

    private byte pv = FrameConstant.pv;

    private long serial = System.currentTimeMillis();

    private byte cmd;

    private int len;

    private Map<String, Object> data = new HashMap<>();

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("cmd:"+ ByteUtil.toHexString(cmd) + "\n");
        sb.append("serial:"+ serial + "\n");
        if (!data.isEmpty()) {
            sb.append("data:" + ByteUtil.toHexString((byte[])data.get("data")) + "\n");
        }
        return sb.toString();
    }
}
