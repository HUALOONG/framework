package cn.jowen.framework.boot.autoconfigure.runtime;

import cn.jowen.framework.data.core.meta.Column;
import cn.jowen.framework.data.core.meta.GeneratedValue;
import cn.jowen.framework.data.core.meta.Id;
import cn.jowen.framework.data.core.meta.Table;
import cn.jowen.framework.data.jdbc.mapping.BeanPropertyRowMapper;
import org.jspecify.annotations.NullMarked;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM 原生镜像提示：JDBC 数据层（实体元注解反射、行映射器构造反射）。
 *
 * @author 王飞
 * @since 2026-08-27
 */
@NullMarked
public class JdbcRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        MemberCategory[] annotations = {
                MemberCategory.INVOKE_PUBLIC_METHODS,
                MemberCategory.INVOKE_DECLARED_METHODS
        };
        for (Class<?> annotation : new Class<?>[]{Table.class, Id.class, Column.class, GeneratedValue.class}) {
            hints.reflection().registerType(annotation, annotations);
        }
        // BeanPropertyRowMapper 反射实例化用户实体并写字段
        hints.reflection().registerType(BeanPropertyRowMapper.class, MemberCategory.INVOKE_PUBLIC_METHODS);
    }
}