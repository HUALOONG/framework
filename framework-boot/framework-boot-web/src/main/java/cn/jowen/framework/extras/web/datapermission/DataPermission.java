package cn.jowen.framework.extras.web.datapermission;

import cn.jowen.framework.extras.properties.DataScope;
import org.jspecify.annotations.NullMarked;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限注解：声明方法的数据可见范围。
 *
 * <p>实际 SQL 条件注入由持久层（MyBatis / JDBC）的适配器结合 {@link DataPermissionRule} 完成，
 * 本注解仅声明意图并写入 {@link DataPermissionContext}。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface DataPermission {

    /** 数据范围 */
    DataScope scope() default DataScope.ALL;

    /** 作用表名（为空表示作用于全部表） */
    String table() default "";
}
