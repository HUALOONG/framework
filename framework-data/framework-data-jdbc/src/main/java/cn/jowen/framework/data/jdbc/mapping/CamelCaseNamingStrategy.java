package cn.jowen.framework.data.jdbc.mapping;

import cn.jowen.framework.data.core.mapping.NamingStrategy;
import org.jspecify.annotations.NullMarked;

/**
 * 驼峰 ↔ 下划线 命名策略。
 *
 * <p>规则：
 * <ul>
 *   <li>字段名 {@code userName} → 列名 {@code user_name}；</li>
 *   <li>类名 {@code User} → 表名 {@code user}；类名 {@code OrderItem} → {@code order_item}；
 *       若类名以 {@code Entity}/{@code DO} 后缀结尾则先去除；</li>
 *   <li>下划线 → 驼峰反向转换（供从元数据读取时使用）。</li>
 * </ul>
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class CamelCaseNamingStrategy implements NamingStrategy {

    /** 常见实体类后缀，转换表名时去除。 */
    private static final String[] SUFFIXES = {"Entity", "DO", "Po", "PO", "Model", "DTO"};

    @Override
    public String toTableName(String className) {
        String name = className;
        for (String suffix : SUFFIXES) {
            if (name.endsWith(suffix) && name.length() > suffix.length()) {
                name = name.substring(0, name.length() - suffix.length());
                break;
            }
        }
        return toSnakeCase(name);
    }

    @Override
    public String toColumnName(String fieldName) {
        return toSnakeCase(fieldName);
    }

    /**
     * 列名下划线转字段名驼峰（用于从数据库元数据读取列名时）。
     *
     * @param columnName 列名，不可为 {@code null}
     * @return 驼峰字段名
     */
    public String toFieldName(String columnName) {
        StringBuilder builder = new StringBuilder();
        boolean upperNext = false;
        for (int i = 0; i < columnName.length(); i++) {
            char c = columnName.charAt(i);
            if (c == '_') {
                upperNext = true;
            } else if (upperNext) {
                builder.append(Character.toUpperCase(c));
                upperNext = false;
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private static String toSnakeCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    builder.append('_');
                }
                builder.append(Character.toLowerCase(c));
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
