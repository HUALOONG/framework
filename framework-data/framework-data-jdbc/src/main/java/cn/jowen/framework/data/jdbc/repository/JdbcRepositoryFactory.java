package cn.jowen.framework.data.jdbc.repository;

import cn.jowen.framework.data.core.dialect.Dialect;
import cn.jowen.framework.data.core.mapping.EntityMetadataResolver;
import cn.jowen.framework.data.core.repository.Repository;
import cn.jowen.framework.data.core.repository.RepositoryFactory;
import org.jspecify.annotations.NullMarked;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * JDBC 仓储工厂，基于 {@link DataSource} 创建 {@link JdbcRepository}。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class JdbcRepositoryFactory implements RepositoryFactory {

    private final JdbcTemplate jdbcTemplate;
    private final EntityMetadataResolver resolver;
    private final Dialect dialect;

    public JdbcRepositoryFactory(DataSource dataSource, EntityMetadataResolver resolver, Dialect dialect) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.resolver = resolver;
        this.dialect = dialect;
    }

    @Override
    public <T, ID> Repository<T, ID> getRepository(Class<T> entityClass) {
        return new JdbcRepository<>(entityClass, jdbcTemplate, resolver, dialect);
    }

    /**
     * 返回底层 {@link JdbcTemplate}（供高级用法）。
     *
     * @return JdbcTemplate，不可为 {@code null}
     */
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }
}
