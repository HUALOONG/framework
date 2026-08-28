package cn.jowen.framework.extras.storage.strategy;

import cn.jowen.framework.extras.properties.NamingStrategy;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * 对象命名策略工厂：按 {@link NamingStrategy} 枚举产出对应策略实例。
 *
 * <p>内置策略为无状态单例，可安全复用；业务方可通过
 * {@link #register(NamingStrategy, ObjectNameStrategy)} 覆盖或扩展自定义策略。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class ObjectNameStrategyFactory {

    /** REGISTRY 常量。 */
    private static final Map<NamingStrategy, ObjectNameStrategy> REGISTRY =
            new EnumMap<>(NamingStrategy.class);

    static {
        REGISTRY.put(NamingStrategy.ORIGINAL, new OriginalNameStrategy());
        REGISTRY.put(NamingStrategy.UUID, new UuidStrategy());
        REGISTRY.put(NamingStrategy.DATE, new DatePathStrategy());
        REGISTRY.put(NamingStrategy.HASH, new HashStrategy());
    }

    private ObjectNameStrategyFactory() {
    }

    /**
     * 获取指定枚举对应的策略。
     *
     * @param type 策略枚举
     * @return 策略实例；未注册时回退到 {@link UuidStrategy}
     */
    public static ObjectNameStrategy get(@Nullable NamingStrategy type) {
        if (type == null) {
            return REGISTRY.get(NamingStrategy.UUID);
        }
        ObjectNameStrategy strategy = REGISTRY.get(type);
        return strategy != null ? strategy : REGISTRY.get(NamingStrategy.UUID);
    }

    /**
     * 注册（或覆盖）自定义策略。
     *
     * @param type     策略枚举
     * @param strategy 策略实现
     */
    public static void register(NamingStrategy type, ObjectNameStrategy strategy) {
        REGISTRY.put(type, strategy);
    }
}
