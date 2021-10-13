package core.netty.group.channel.strategy.constant;

import core.netty.group.channel.strategy.KeyBasedForkStrategy;
import core.netty.group.channel.strategy.MinLoadForkStrategy;
import core.netty.group.channel.strategy.RandomForkStrategy;
import core.netty.group.channel.strategy.RoundRobinForkStrategy;
import lombok.Getter;

/**
 * @author wneck130@gmail.com
 * @Description:
 * @date 2021/9/28
 */
@Getter
public enum ForkStrategyEnum {

    RANDOM("随机获取", RandomForkStrategy.class),
    MIN_LOAD("最小负载", MinLoadForkStrategy.class),
    ROUND_ROBIN("轮询", RoundRobinForkStrategy.class),
    KEY("根据Key获取", KeyBasedForkStrategy.class);

    /**
     * 策略名
     */
    private String name;

    /**
     * 策略实现类
     */
    private Class clazz;

    ForkStrategyEnum (String name, Class clazz) {
        this.name = name;
        this.clazz = clazz;
    }
}
