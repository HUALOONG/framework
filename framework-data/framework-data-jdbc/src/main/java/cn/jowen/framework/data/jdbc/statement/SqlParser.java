package cn.jowen.framework.data.jdbc.statement;

import cn.jowen.framework.core.util.StringUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 轻量 SQL 解析工具。
 *
 * <p>提供命名参数（{@code :name}）提取、分页占位识别等能力，供 {@code NamedParameterTemplate} 等组件使用。
 * 仅做语法层轻解析，不解析完整 SQL 语法。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class SqlParser {

    /** 命名参数正则：:后跟字母/数字/下划线。 */
    private static final Pattern NAMED_PARAM_PATTERN = Pattern.compile(":([a-zA-Z_][a-zA-Z0-9_]*)");

    private SqlParser() {
    }

    /**
     * 提取 SQL 中的命名参数名（按出现顺序，去重）。
     *
     * @param sql 含命名参数的 SQL，可为 {@code null}
     * @return 参数名有序集合（不含冒号），不可为 {@code null}
     */
    public static List<String> parseNamedParameters(@Nullable String sql) {
        List<String> ordered = new ArrayList<>();
        if (StringUtils.isEmpty(sql)) {
            return ordered;
        }
        Set<String> seen = new LinkedHashSet<>();
        Matcher matcher = NAMED_PARAM_PATTERN.matcher(sql);
        while (matcher.find()) {
            String name = matcher.group(1);
            if (seen.add(name)) {
                ordered.add(name);
            }
        }
        return ordered;
    }

    /**
     * 判断 SQL 是否包含命名参数。
     *
     * @param sql SQL，可为 {@code null}
     * @return 包含返回 {@code true}
     */
    public static boolean hasNamedParameters(@Nullable String sql) {
        if (StringUtils.isEmpty(sql)) {
            return false;
        }
        return NAMED_PARAM_PATTERN.matcher(sql).find();
    }

    /**
     * 将命名参数 SQL 转为 {@code ?} 占位 SQL，并返回参数名顺序（与 {@link #parseNamedParameters} 一致）。
     *
     * @param sql 命名参数 SQL，不可为 {@code null}
     * @return 转换后的 {@code ?} 占位 SQL
     */
    public static String toPositionalSql(String sql) {
        if (!hasNamedParameters(sql)) {
            return sql;
        }
        Matcher matcher = NAMED_PARAM_PATTERN.matcher(sql);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, "?");
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
