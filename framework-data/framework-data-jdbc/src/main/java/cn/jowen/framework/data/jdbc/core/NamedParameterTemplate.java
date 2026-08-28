package cn.jowen.framework.data.jdbc.core;

import cn.jowen.framework.data.core.mapping.RowMapper;
import cn.jowen.framework.core.util.ReflectionUtils;
import cn.jowen.framework.data.jdbc.statement.SqlParser;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 命名参数模板：支持 {@code :name} 命名参数（将 {@link Map} 或 Bean 解析为 {@code ?} 占位 + 有序参数）。
 *
 * <p>底层委托 {@link JdbcOperations} 执行；SQL 中的 {@code :name} 被替换为 {@code ?}，参数按出现顺序从
 * {@code Map}（key 为参数名）或 Bean（字段名即参数名）中提取。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class NamedParameterTemplate {

    private final JdbcOperations jdbcOperations;

    public NamedParameterTemplate(JdbcOperations jdbcOperations) {
        this.jdbcOperations = jdbcOperations;
    }

    /**
     * 命名参数查询并映射。
     *
     * @param sql     含 {@code :name} 的 SQL，不可为 {@code null}
     * @param rm      行映射器，不可为 {@code null}
     * @param params  命名参数 Map，不可为 {@code null}
     * @param <T>     类型
     * @return 对象列表
     */
    public <T> List<T> query(String sql, RowMapper<T> rm, Map<String, Object> params) {
        Parsed parsed = parse(sql);
        return jdbcOperations.query(parsed.sql, rm, orderParams(parsed.names, params).toArray());
    }

    /**
     * 命名参数查询单行。
     *
     * @param sql     含 {@code :name} 的 SQL，不可为 {@code null}
     * @param rm      行映射器，不可为 {@code null}
     * @param params  命名参数 Map，不可为 {@code null}
     * @param <T>     类型
     * @return 单个对象
     */
    public <T> T queryForObject(String sql, RowMapper<T> rm, Map<String, Object> params) {
        Parsed parsed = parse(sql);
        return jdbcOperations.queryForObject(parsed.sql, rm, orderParams(parsed.names, params).toArray());
    }

    /**
     * 命名参数更新/删除。
     *
     * @param sql    含 {@code :name} 的 SQL，不可为 {@code null}
     * @param params 命名参数 Map，不可为 {@code null}
     * @return 受影响行数
     */
    public int update(String sql, Map<String, Object> params) {
        Parsed parsed = parse(sql);
        return jdbcOperations.update(parsed.sql, orderParams(parsed.names, params).toArray());
    }

    /**
     * 以 Bean 为参数源执行查询。
     *
     * @param sql  含 {@code :name} 的 SQL，不可为 {@code null}
     * @param rm   行映射器，不可为 {@code null}
     * @param bean 参数 Bean，不可为 {@code null}
     * @param <T>  类型
     * @return 对象列表
     */
    public <T> List<T> query(String sql, RowMapper<T> rm, Object bean) {
        return query(sql, rm, beanToMap(bean));
    }

    /**
     * 以 Bean 为参数源执行更新。
     *
     * @param sql  含 {@code :name} 的 SQL，不可为 {@code null}
     * @param bean 参数 Bean，不可为 {@code null}
     * @return 受影响行数
     */
    public int update(String sql, Object bean) {
        return update(sql, beanToMap(bean));
    }

    private Parsed parse(String sql) {
        List<String> names = SqlParser.parseNamedParameters(sql);
        return new Parsed(SqlParser.toPositionalSql(sql), names);
    }

    private List<Object> orderParams(List<String> names, Map<String, Object> params) {
        List<Object> ordered = new ArrayList<>(names.size());
        for (String name : names) {
            if (!params.containsKey(name)) {
                throw new IllegalArgumentException("命名参数缺失：" + name);
            }
            ordered.add(params.get(name));
        }
        return ordered;
    }

    private static Map<String, Object> beanToMap(Object bean) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Field field : ReflectionUtils.getAllFields(bean.getClass())) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            map.put(field.getName(), ReflectionUtils.getFieldValue(bean, field.getName()));
        }
        return map;
    }

    private record Parsed(String sql, List<String> names) {
    }

    @Nullable
    private static Object unused() {
        return null;
    }
}
