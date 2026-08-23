package cn.jowen.framework.data.jdbc.repository;

import cn.jowen.framework.data.core.exception.DataAccessException;
import cn.jowen.framework.data.core.mapping.EntityMetadata;
import cn.jowen.framework.data.core.mapping.EntityMetadataResolver;
import cn.jowen.framework.data.core.mapping.PropertyMetadata;
import cn.jowen.framework.data.core.mapping.RowMapper;
import cn.jowen.framework.data.core.page.Page;
import cn.jowen.framework.data.core.page.Pageable;
import cn.jowen.framework.data.core.page.Sort;
import cn.jowen.framework.data.core.query.QueryWrapper;
import cn.jowen.framework.core.util.ReflectionUtils;
import cn.jowen.framework.data.core.dialect.DatabaseDialect;
import cn.jowen.framework.data.core.meta.GeneratedValue;
import cn.jowen.framework.data.jdbc.core.JdbcTemplate;
import cn.jowen.framework.data.jdbc.dialect.DialectRegistry;
import cn.jowen.framework.data.jdbc.mapping.BeanPropertyRowMapper;
import cn.jowen.framework.data.jdbc.statement.SqlResult;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 简单 JDBC 仓储实现：基于 {@link JdbcTemplate} 落地 CRUD / 分页 / 动态 SQL。
 *
 * <p>通过 {@link EntityMetadataResolver} 解析实体元信息，{@link DialectRegistry} 适配分页方言，
 * {@link IdGenerator} 与 {@link QueryWrapperTranslator} 完成主键生成与动态条件翻译。
 *
 * @param <T>  实体类型
 * @param <ID> 主键类型
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public class SimpleJdbcRepository<T, ID> implements JdbcRepository<T, ID> {

    private final Class<T> entityClass;
    private final EntityMetadata meta;
    private final RowMapper<T> rowMapper;
    private final JdbcTemplate jdbcTemplate;
    private final DialectRegistry dialectRegistry;
    private final IdGenerator idGenerator;
    private final QueryWrapperTranslator translator;

    public SimpleJdbcRepository(JdbcTemplate jdbcTemplate, EntityMetadataResolver resolver,
                                DialectRegistry dialectRegistry, IdGenerator idGenerator, Class<T> entityClass) {
        this.entityClass = entityClass;
        this.meta = resolver.resolve(entityClass);
        this.rowMapper = BeanPropertyRowMapper.of(entityClass, resolver);
        this.jdbcTemplate = jdbcTemplate;
        this.dialectRegistry = dialectRegistry;
        this.idGenerator = idGenerator;
        this.translator = new QueryWrapperTranslator();
    }

    public IdGenerator getIdGenerator() {
        return idGenerator;
    }

    private String tableName() {
        return meta.getTableName();
    }

    private @Nullable PropertyMetadata idProperty() {
        String idColumn = meta.getIdColumn();
        return idColumn == null ? null : meta.getProperty(idColumn);
    }

    @Override
    public Optional<T> findById(ID id) {
        String sql = "SELECT * FROM " + tableName() + " WHERE " + requireIdColumn() + " = ?";
        List<T> list = jdbcTemplate.query(sql, rowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<T> findAll() {
        return jdbcTemplate.query("SELECT * FROM " + tableName(), rowMapper);
    }

    @Override
    public Page<T> findAll(Pageable pageable) {
        return page(pageable);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T save(T entity) {
        PropertyMetadata idProp = idProperty();
        Object idVal = idProp == null ? null : ReflectionUtils.getFieldValue(entity, idProp.getName());
        if (idProp != null && idProp.isGenerated()) {
            GeneratedValue.Strategy strat = idProp.getGenerated().orElse(GeneratedValue.Strategy.AUTO);
            if (strat == GeneratedValue.Strategy.AUTO) {
                insertExcludingId(entity, idProp);
            } else {
                Object generated = IdGenerators.get(strat).generate(meta, entity);
                ReflectionUtils.setFieldValue(entity, idProp.getName(), generated);
                insertIncludingId(entity);
            }
        } else {
            if (idVal == null) {
                insertExcludingId(entity, idProp);
            } else if (existsById((ID) idVal)) {
                update(entity);
            } else {
                insertIncludingId(entity);
            }
        }
        return entity;
    }

    @Override
    public List<T> saveAll(List<T> entities) {
        List<T> result = new ArrayList<>(entities.size());
        for (T entity : entities) {
            result.add(save(entity));
        }
        return result;
    }

    @Override
    public T update(T entity) {
        PropertyMetadata idProp = idProperty();
        if (idProp == null) {
            throw new DataAccessException("实体无主键，无法执行 update：" + entityClass.getName());
        }
        List<PropertyMetadata> settable = new ArrayList<>();
        for (PropertyMetadata p : meta.getProperties()) {
            if (!p.isId()) {
                settable.add(p);
            }
        }
        if (settable.isEmpty()) {
            throw new DataAccessException("实体无可更新字段：" + entityClass.getName());
        }
        StringBuilder sql = new StringBuilder("UPDATE ").append(tableName()).append(" SET ");
        List<Object> params = new ArrayList<>();
        for (int i = 0; i < settable.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(settable.get(i).getColumnName()).append(" = ?");
            params.add(ReflectionUtils.getFieldValue(entity, settable.get(i).getName()));
        }
        sql.append(" WHERE ").append(idProp.getColumnName()).append(" = ?");
        params.add(ReflectionUtils.getFieldValue(entity, idProp.getName()));
        jdbcTemplate.update(sql.toString(), params.toArray());
        return entity;
    }

    @Override
    public int deleteById(ID id) {
        return jdbcTemplate.update("DELETE FROM " + tableName() + " WHERE " + requireIdColumn() + " = ?", id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public int delete(T entity) {
        PropertyMetadata idProp = idProperty();
        if (idProp == null) {
            throw new DataAccessException("实体无主键，无法执行 delete：" + entityClass.getName());
        }
        Object idVal = ReflectionUtils.getFieldValue(entity, idProp.getName());
        return deleteById((ID) idVal);
    }

    @Override
    public int deleteAll() {
        return jdbcTemplate.update("DELETE FROM " + tableName());
    }

    @Override
    public boolean existsById(ID id) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName() + " WHERE " + requireIdColumn() + " = ?", Long.class, id);
        return count != null && count > 0;
    }

    @Override
    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName(), Long.class);
        return count == null ? 0L : count;
    }

    @Override
    public Page<T> page(Pageable pageable) {
        String base = "SELECT * FROM " + tableName();
        return pageInternal(appendSort(base, pageable.getSort()), List.of(), pageable, false);
    }

    @Override
    public Page<T> page(Pageable pageable, QueryWrapper<T> wrapper) {
        SqlResult result = translator.translate(wrapper, tableName());
        return pageInternal(result.sql(), result.params(), pageable, true);
    }

    private Page<T> pageInternal(String baseSql, List<Object> params, Pageable pageable, boolean alreadyOrdered) {
        String countSql = "SELECT COUNT(*) FROM (" + stripOrderBy(baseSql) + ") AS _cnt";
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        DatabaseDialect dialect = dialectRegistry.getDefault();
        String pageSql = dialect.buildPageSql(baseSql, pageable);
        List<T> content = jdbcTemplate.query(pageSql, rowMapper, params.toArray());
        return new Page<>(content, total == null ? 0L : total, pageable.getPage(), pageable.getSize());
    }

    private String appendSort(String base, Sort sort) {
        if (sort.isEmpty()) {
            return base;
        }
        StringBuilder sb = new StringBuilder(base).append(" ORDER BY ");
        List<Sort.Order> orders = sort.getOrders();
        for (int i = 0; i < orders.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(orders.get(i).toSql());
        }
        return sb.toString();
    }

    private static String stripOrderBy(String sql) {
        int idx = sql.toUpperCase().lastIndexOf(" ORDER BY ");
        return idx < 0 ? sql : sql.substring(0, idx);
    }

    private void insertIncludingId(T entity) {
        doInsert(entity, false, null);
    }

    private void insertExcludingId(T entity, @Nullable PropertyMetadata idProp) {
        doInsert(entity, true, idProp);
    }

    private void doInsert(T entity, boolean excludeId, @Nullable PropertyMetadata idProp) {
        List<PropertyMetadata> cols = new ArrayList<>();
        for (PropertyMetadata p : meta.getProperties()) {
            if (excludeId && p.isId()) {
                continue;
            }
            cols.add(p);
        }
        if (cols.isEmpty()) {
            throw new DataAccessException("实体无可插入字段：" + entityClass.getName());
        }
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName()).append(" (");
        StringBuilder placeholders = new StringBuilder(" VALUES (");
        List<Object> params = new ArrayList<>();
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) {
                sql.append(", ");
                placeholders.append(", ");
            }
            sql.append(cols.get(i).getColumnName());
            placeholders.append("?");
            params.add(ReflectionUtils.getFieldValue(entity, cols.get(i).getName()));
        }
        sql.append(")").append(placeholders).append(")");
        Object key = jdbcTemplate.executeInsert(sql.toString(), params.toArray());
        if (excludeId && idProp != null && key != null) {
            ReflectionUtils.setFieldValue(entity, idProp.getName(),
                    BeanPropertyRowMapper.convert(key, idProp.getJavaType()));
        }
    }

    private String requireIdColumn() {
        String idColumn = meta.getIdColumn();
        if (idColumn == null) {
            throw new DataAccessException("实体无主键列：" + entityClass.getName());
        }
        return idColumn;
    }

    @Override
    public List<Map<String, Object>> query(String sql, Object... params) {
        return jdbcTemplate.queryForMaps(sql, params);
    }

    @Override
    public int update(String sql, Object... params) {
        return jdbcTemplate.update(sql, params);
    }

    @Override
    public @Nullable Map<String, Object> queryOne(String sql, Object... params) {
        List<Map<String, Object>> list = jdbcTemplate.queryForMaps(sql, params);
        return list.isEmpty() ? null : list.get(0);
    }
}
