package cn.jowen.framework.extras.web.operatelog;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解：记录方法级操作行为（谁在何时做了什么、耗时与结果）。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface OperateLog {

    /** 操作名称 */
    String value() default "";

    /** 所属模块 */
    String module() default "";

    /** 是否异步记录（依赖 handler 实现） */
    boolean async() default true;

    /** 是否记录入参 */
    boolean recordParams() default false;

    /** 是否记录返回值 */
    boolean recordResult() default false;
}
