package cn.jowen.framework.data.core.mapping;

import org.jspecify.annotations.NullMarked;

import java.sql.Types;

/**
 * JDBC 类型枚举，映射 {@code java.sql.Types} 常量。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public enum JdbcType {
    VARCHAR(Types.VARCHAR),
    CHAR(Types.CHAR),
    CLOB(Types.CLOB),
    TEXT(Types.LONGVARCHAR),
    INTEGER(Types.INTEGER),
    BIGINT(Types.BIGINT),
    SMALLINT(Types.SMALLINT),
    TINYINT(Types.TINYINT),
    FLOAT(Types.FLOAT),
    DOUBLE(Types.DOUBLE),
    DECIMAL(Types.DECIMAL),
    NUMERIC(Types.NUMERIC),
    BOOLEAN(Types.BIT),
    DATE(Types.DATE),
    TIME(Types.TIME),
    TIMESTAMP(Types.TIMESTAMP),
    BLOB(Types.BLOB),
    BINARY(Types.BINARY),
    VARBINARY(Types.VARBINARY),
    LONGVARBINARY(Types.LONGVARBINARY),
    NULL(Types.NULL),
    OTHER(Types.OTHER);

    private final int code;

    JdbcType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * 从 {@code java.sql.Types} 常量获取对应枚举值。
     *
     * @param code JDBC 类型码
     * @return 对应枚举；未知类型返回 {@code OTHER}
     */
    public static JdbcType of(int code) {
        for (JdbcType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return OTHER;
    }
}
