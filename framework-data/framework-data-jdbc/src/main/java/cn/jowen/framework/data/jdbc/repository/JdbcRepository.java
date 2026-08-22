package cn.jowen.framework.data.jdbc.repository;

import cn.jowen.framework.data.core.dialect.Dialect;
import cn.jowen.framework.data.core.exception.DataException;
import cn.jowen.framework.data.core.mapping.EntityMetadata;
import cn.jowen.framework.data.core.mapping.EntityMetadataResolver;
import cn.jowen.framework.data.core.page.Page;
import cn.jowen.framework.data.core.page.Pageable;
import cn.jowen.framework.data.core.query.Query;
import cn.jowen.framework.data.core.repository.QueryRepository;
import cn.jowen.framework.data.core.repository.Repository;
import cn.jowen.framework.data.jdbc.mapping.BeanRowMapper;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 基于 Spring {@link JdbcTemplate} 的通用仓储实现。本类仅接收 core 抽象，不向上暴露 Spring 类型。
 *
 * @param <T>  实体类型
 * @param <ID> 主键类型
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public class JdbcRepository<T, ID> implements Repository<T, ID>, QueryRepository<T> {

    private final JdbcTemplate jdbcTemplate;
    private final EntityMetadataResolver resolver;
    private final Dialect dialect;
    private final BeanRowMapper<T> rowMapper;
    private final EntityMetadata metadata;
    private final Class<T> entityClass;

    public JdbcRepository(Class<T> entityClass, JdbcTemplate jdbcTemplate,
                          EntityMetadataResolver resolver, Dialect dialect) {
        this.entityClass = entityClass;
        this.jdbcTemplate = jdbcTemplate;
        this.resolver = resolver;
        this.dialect = dialect;
        this.metadata = resolver.resolve(entityClass);
        this.rowMapper = new BeanRowMapper<>(entityClass, resolver);
    }

    @Override
    public Optional<T> findById(ID id) {
        String sql = "SELECT * FROM " + metadata.getTableName() + " WHERE " + requireId() + " = ?";
        List<T> list = queryForList(sql, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<T> findAll() {
        return queryForList("SELECT * FROM " + metadata.getTableName());
    }

    @Override
    public Page<T> findAll(Pageable pageable) {
        String base = "SELECT * FROM " + metadata.getTableName();
        String pageSql = dialect.buildPageSql(base, pageable);
        List<T> content = queryForList(pageSql);
        long total = count();
        return new Page<>(content, total, pageable.getPage(), pageable.getSize());
    }

    @Override
    public T save(T entity) {
        Map<String, Object> values = extractValues(entity);
        if (metadata.getIdColumn() != null && isNull(values.get(metadata.getIdColumn()))) {
            return insert(entity, values);
        }
        return update(entity, values);
    }

    @Override
    public List<T> saveAll(List<T> entities) {
        List<T> result = new ArrayList<>(entities.size());
        for (T e : entities) {
            result.add(save(e));
        }
        return result;
    }

    @Override
    public int deleteById(ID id) {
        String sql = "DELETE FROM " + metadata.getTableName() + " WHERE " + requireId() + " = ?";
        return jdbcTemplate.update(sql, id);
    }

    @Override
    public boolean existsById(ID id) {
        return findById(id).isPresent();
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM " + metadata.getTableName();
        Long n = jdbcTemplate.queryForObject(sql, Long.class);
        return n == null ? 0L : n;
    }

    @Override
    public List<T> findBy(Query query) {
        StringBuilder where = new StringBuilder();
        List<Object> args = new ArrayList<>();
        buildWhere(query, where, args, true);
        String sql = "SELECT * FROM " + metadata.getTableName() + where;
        return queryForList(sql, args.toArray());
    }

    @Override
    public Page<T> findBy(Query query, Pageable pageable) {
        StringBuilder where = new StringBuilder();
        List<Object> args = new ArrayList<>();
        buildWhere(query, where, args, false);
        String base = "SELECT * FROM " + metadata.getTableName() + where;
        String pageSql = dialect.buildPageSql(base, pageable);
        List<T> content = queryForList(pageSql, args.toArray());
        long total = countBy(query);
        return new Page<>(content, total, pageable.getPage(), pageable.getSize());
    }

    @Override
    public long countBy(Query query) {
        StringBuilder where = new StringBuilder();
        List<Object> args = new ArrayList<>();
        buildWhere(query, where, args, false);
        String sql = dialect.buildCountSql("SELECT * FROM " + metadata.getTableName() + where);
        Long n = jdbcTemplate.queryForObject(sql, Long.class, args.toArray());
        return n == null ? 0L : n;
    }

    // ----- 内部实现 -----

    private T insert(T entity, Map<String, Object> values) {
        List<String> cols = new ArrayList<>(metadata.getColumnNames());
        cols.removeIf(c -> c.equals(metadata.getIdColumn()) && isNull(values.get(c)));
        String colStr = String.join(", ", cols);
        String placeholder = ", ?".repeat(cols.size()).replaceFirst(", ", "");
        String sql = "INSERT INTO " + metadata.getTableName() + " (" + colStr + ") VALUES (" + placeholder + ")";
        List<Object> params = new ArrayList<>();
        for (String c : cols) {
            params.add(values.get(c));
        }
        if (metadata.getIdColumn() != null && GeneratedKeyHolderSupported()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(con -> {
                PreparedStatement ps = con.prepareStatement(sql, new String[]{metadata.getIdColumn()});
                setParams(ps, params);
                return ps;
            }, keyHolder);
            assignId(entity, keyHolder.getKey());
            return entity;
        }
        jdbcTemplate.update(sql, params.toArray());
        return entity;
    }

    private T update(T entity, Map<String, Object> values) {
        List<String> cols = new ArrayList<>(metadata.getColumnNames());
        String idCol = requireId();
        cols.remove(idCol);
        StringBuilder sb = new StringBuilder("UPDATE ").append(metadata.getTableName()).append(" SET ");
        List<Object> params = new ArrayList<>();
        boolean first = true;
        for (String c : cols) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(c).append(" = ?");
            params.add(values.get(c));
            first = false;
        }
        sb.append(" WHERE ").append(idCol).append(" = ?");
        params.add(values.get(idCol));
        jdbcTemplate.update(sb.toString(), params.toArray());
        return entity;
    }

    private void buildWhere(Query query, StringBuilder where, List<Object> args, boolean withSort) {
        boolean first = true;
        for (Query.Condition c : query.getConditions()) {
            if (!first) {
                where.append(" AND ");
            } else {
                where.append(" WHERE ");
            }
            appendCondition(where, args, c);
            first = false;
        }
        if (withSort && !query.getSort().isEmpty()) {
            where.append(" ORDER BY ");
            boolean f = true;
            for (cn.jowen.framework.data.core.page.Sort.Order o : query.getSort().getOrders()) {
                if (!f) {
                    where.append(", ");
                }
                where.append(o.toSql());
                f = false;
            }
        }
        if (query.getLimit() != null) {
            where.append(" LIMIT ").append(query.getLimit());
        }
    }

    private void appendCondition(StringBuilder where, List<Object> args, Query.Condition c) {
        where.append(c.getColumn());
        switch (c.getOperator()) {
            case EQ -> {
                if (c.getValue() == null) {
                    where.append(" IS NULL");
                } else {
                    where.append(" = ?");
                    args.add(c.getValue());
                }
            }
            case NE -> {
                if (c.getValue() == null) {
                    where.append(" IS NOT NULL");
                } else {
                    where.append(" <> ?");
                    args.add(c.getValue());
                }
            }
            case LIKE -> {
                where.append(" LIKE ?");
                args.add("%" + c.getValue() + "%");
            }
            case GT -> {
                where.append(" > ?");
                args.add(c.getValue());
            }
            case LT -> {
                where.append(" < ?");
                args.add(c.getValue());
            }
            case GTE -> {
                where.append(" >= ?");
                args.add(c.getValue());
            }
            case LTE -> {
                where.append(" <= ?");
                args.add(c.getValue());
            }
            case IN -> {
                where.append(" IN (?)");
                args.add(c.getValue());
            }
        }
    }

    private List<T> queryForList(String sql, Object... args) {
        List<T> result = new ArrayList<>();
        org.springframework.jdbc.core.RowCallbackHandler handler = rs -> mapRowTo(result, rs);
        jdbcTemplate.query(sql, handler, args);
        return result;
    }

    private void mapRowTo(List<T> result, ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        int cols = rs.getMetaData().getColumnCount();
        for (int i = 1; i <= cols; i++) {
            row.put(rs.getMetaData().getColumnLabel(i).toLowerCase(), rs.getObject(i));
        }
        T entity = rowMapper.mapRow(row, result.size());
        if (entity != null) {
            result.add(entity);
        }
    }

    private Map<String, Object> extractValues(T entity) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String col : metadata.getColumnNames()) {
            String fieldName = metadata.getFieldName(col);
            if (fieldName == null) {
                values.put(col, null);
                continue;
            }
            try {
                java.lang.reflect.Field f = entityClass.getDeclaredField(fieldName);
                f.setAccessible(true);
                values.put(col, f.get(entity));
            } catch (ReflectiveOperationException e) {
                throw new DataException("读取字段失败 " + fieldName, e);
            }
        }
        return values;
    }

    private void assignId(T entity, @Nullable Number key) {
        if (key == null || metadata.getIdColumn() == null) {
            return;
        }
        String fieldName = metadata.getFieldName(metadata.getIdColumn());
        if (fieldName == null) {
            return;
        }
        try {
            java.lang.reflect.Field f = entityClass.getDeclaredField(fieldName);
            f.setAccessible(true);
            Class<?> t = f.getType();
            Object v = (t == Long.class || t == long.class) ? key.longValue()
                    : (t == Integer.class || t == int.class) ? key.intValue() : key;
            f.set(entity, v);
        } catch (ReflectiveOperationException e) {
            throw new DataException("回写主键失败 " + fieldName, e);
        }
    }

    private void setParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private boolean GeneratedKeyHolderSupported() {
        return true;
    }

    private String requireId() {
        if (metadata.getIdColumn() == null) {
            throw new DataException("实体 " + entityClass.getName() + " 未声明 @Id 主键");
        }
        return metadata.getIdColumn();
    }

    private boolean isNull(@Nullable Object v) {
        return v == null;
    }
}
