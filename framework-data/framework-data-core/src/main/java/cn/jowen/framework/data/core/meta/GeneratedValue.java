package cn.jowen.framework.data.core.meta;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记主键生成策略。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface GeneratedValue {

    /**
     * 生成策略。
     *
     * @return 策略
     */
    Strategy value() default Strategy.AUTO;

    /** 主键生成策略枚举。 */
    enum Strategy {
        /** 自增（数据库 IDENTITY）。 */
        AUTO,
        /** 雪花算法。 */
        SNOWFLAKE,
        /** UUID。 */
        UUID,
        /** 业务自定义，需提供生成器。 */
        ASSIGNED
    }
}
