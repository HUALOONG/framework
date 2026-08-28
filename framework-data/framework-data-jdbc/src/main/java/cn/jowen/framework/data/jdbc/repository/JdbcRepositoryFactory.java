package cn.jowen.framework.data.jdbc.repository;

import cn.jowen.framework.core.spi.SPIImplementation;
import cn.jowen.framework.data.jdbc.dialect.DialectRegistry;
import cn.jowen.framework.data.core.mapping.EntityMetadataResolver;
import cn.jowen.framework.data.core.repository.Repository;
import cn.jowen.framework.data.core.repository.RepositoryFactory;
import cn.jowen.framework.data.jdbc.core.JdbcTemplate;
import org.jspecify.annotations.NullMarked;

/**
 * JDBC 仓储工厂（SPI 实现）：按实体类型创建 {@link SimpleJdbcRepository} 实例。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@SPIImplementation(name = "jdbc")
@NullMarked
public final class JdbcRepositoryFactory implements RepositoryFactory {

    private final JdbcTemplate jdbcTemplate;
    private final EntityMetadataResolver resolver;
    private final DialectRegistry dialectRegistry;
    private final IdGenerator idGenerator;

    public JdbcRepositoryFactory(JdbcTemplate jdbcTemplate, EntityMetadataResolver resolver,
                                 DialectRegistry dialectRegistry, IdGenerator idGenerator) {
        this.jdbcTemplate = jdbcTemplate;
        this.resolver = resolver;
        this.dialectRegistry = dialectRegistry;
        this.idGenerator = idGenerator;
    }

    @Override
    public <T, ID> Repository<T, ID> getRepository(Class<T> entityClass) {
        return new SimpleJdbcRepository<>(jdbcTemplate, resolver, dialectRegistry, idGenerator, entityClass);
    }
}
