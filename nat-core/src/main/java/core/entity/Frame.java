package core.entity;

import core.constant.FrameConstant;
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
}
