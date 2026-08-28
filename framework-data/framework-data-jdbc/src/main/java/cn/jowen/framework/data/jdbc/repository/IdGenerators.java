package cn.jowen.framework.data.jdbc.repository;

import cn.jowen.framework.data.core.meta.GeneratedValue;
import org.jspecify.annotations.NullMarked;

/**
 * 主键生成器工厂：将 core 的 {@link GeneratedValue.Strategy} 映射为默认 {@link IdGenerator} 实例。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class IdGenerators {

    private IdGenerators() {
    }

    /**
     * 按 core 生成策略创建生成器。
     *
     * @param strategy core 生成策略，不可为 {@code null}
     * @return 生成器，不可为 {@code null}
     */
    public static IdGenerator get(GeneratedValue.Strategy strategy) {
        return switch (strategy) {
            case AUTO -> new DefaultIdGenerator(IdGenerator.Strategy.AUTO);
            case UUID -> new DefaultIdGenerator(IdGenerator.Strategy.UUID);
            case SNOWFLAKE -> new DefaultIdGenerator(IdGenerator.Strategy.SNOWFLAKE);
            case ASSIGNED -> new DefaultIdGenerator(IdGenerator.Strategy.ASSIGNED);
        };
    }
}
