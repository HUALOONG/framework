package cn.jowen.framework.data.core.mapping;

import cn.jowen.framework.data.core.meta.GeneratedValue;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * 实体属性元数据，描述单个字段（或列）的持久化信息，如列名、是否主键、是否可空、JDBC 类型等。
 *
 * @param name          字段名（或列名）
 * @param javaType      Java 类型
 * @param jdbcType      JDBC 类型，可为 {@code null}（由框架自动推断）
 * @param isId          是否主键
 * @param isNullable    是否可空
 * @param columnName    对应数据库列名
 * @param generated     主键生成策略，为空表示无生成策略
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class PropertyMetadata {

    private final String name;
    private final Class<?> javaType;
    private final @Nullable JdbcType jdbcType;
    private final boolean isId;
    private final boolean isNullable;
    private final String columnName;
    private final Optional<GeneratedValue.Strategy> generated;

    public PropertyMetadata(String name, Class<?> javaType, @Nullable JdbcType jdbcType,
                            boolean isId, boolean isNullable,
                            String columnName, Optional<GeneratedValue.Strategy> generated) {
        this.name = name;
        this.javaType = javaType;
        this.jdbcType = jdbcType;
        this.isId = isId;
        this.isNullable = isNullable;
        this.columnName = columnName;
        this.generated = generated;
    }

    public String getName() { return name; }

    public Class<?> getJavaType() { return javaType; }

    @Nullable
    public JdbcType getJdbcType() { return jdbcType; }

    public boolean isId() { return isId; }

    public boolean isNullable() { return isNullable; }

    public String getColumnName() { return columnName; }

    public Optional<GeneratedValue.Strategy> getGenerated() { return generated; }

    public boolean isGenerated() {
        return isId && generated.isPresent();
    }
}
