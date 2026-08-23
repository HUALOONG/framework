package cn.jowen.framework.data.jdbc.repository;

import cn.jowen.framework.data.core.mapping.EntityMetadata;
import cn.jowen.framework.core.spi.SPI;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 主键生成器（SPI 扩展点）：为实体生成主键值。
 *
 * <p>内置策略见 {@link Strategy}：{@code AUTO}（交由数据库自增，generate 返回 {@code null}）、
 * {@code UUID}、{@code SNOWFLAKE}、{@code SEQUENCE}、{@code ASSIGNED}（使用实体已有值）。
 * 自定义策略可实现本接口并经 {@code ExtensionLoader} 发现。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@SPI
@NullMarked
public interface IdGenerator {

    /**
     * 生成主键。
     *
     * @param meta   实体元信息，不可为 {@code null}
     * @param entity 实体实例，不可为 {@code null}
     * @return 生成的主键值；{@code AUTO} 策略下返回 {@code null}（表示由数据库生成）
     */
    @Nullable
    Object generate(EntityMetadata meta, Object entity);

    /**
     * 主键生成策略。
     */
    enum Strategy {
        /** 数据库自增，不主动生成。 */
        AUTO,
        /** UUID 字符串。 */
        UUID,
        /** 雪花算法（长整型）。 */
        SNOWFLAKE,
        /** 序列（此处以本地计数器模拟）。 */
        SEQUENCE,
        /** 由调用方显式赋值。 */
        ASSIGNED
    }
}
