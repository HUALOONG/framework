package cn.jowen.framework.boot.autoconfigure.data;

import cn.jowen.framework.data.core.datasource.PoolType;
import cn.jowen.framework.data.core.mapping.EntityMetadataResolver;
import cn.jowen.framework.data.core.repository.RepositoryFactory;
import cn.jowen.framework.data.core.transaction.TransactionManager;
import cn.jowen.framework.data.jdbc.connection.ConnectionProvider;
import cn.jowen.framework.data.jdbc.config.JdbcProperties;
import cn.jowen.framework.data.jdbc.core.BatchTemplate;
import cn.jowen.framework.data.jdbc.core.JdbcTemplate;
import cn.jowen.framework.data.jdbc.core.NamedParameterTemplate;
import cn.jowen.framework.data.jdbc.core.SqlRunner;
import cn.jowen.framework.data.jdbc.dialect.DialectRegistry;
import cn.jowen.framework.data.jdbc.repository.JdbcRepositoryFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link JdbcAutoConfiguration} 集成测试：在无 Spring DataSource 的场景下，
 * 验证模块可通过 {@code framework.data.datasource.url} 配置独立构建完整 JDBC 栈。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class JdbcAutoConfigurationTest {

    private static final String URL = "jdbc:h2:mem:jowen-data-jdbc;DB_CLOSE_DELAY=-1";

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JdbcAutoConfiguration.class))
            .withInitializer(ctx -> {
                YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
                yaml.setResources(new ClassPathResource("application.yaml"));
                Properties props = yaml.getObject();
                if (props != null) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    props.forEach((k, v) -> map.put(k.toString(), v));
                    ctx.getEnvironment().getPropertySources().addLast(
                        new MapPropertySource("classpath:application.yaml", map));
                }
            })
            .withPropertyValues(
                    "framework.data.enabled=true",
                    "framework.data.type=jdbc",
                    "framework.data.datasource.url=" + URL,
                    "framework.data.datasource.pool-type=SIMPLE",
                    "framework.data.datasource.maximum-pool-size=5"
            );

    @Test
    void shouldBindDataSourcePropertiesAndCreateConnectionProvider() {
        context.run(ctx -> {
            assertThat(ctx).hasSingleBean(BootDataSourceProperties.class);
            BootDataSourceProperties binder = ctx.getBean(BootDataSourceProperties.class);
            assertThat(binder.getUrl()).isEqualTo(URL);
            assertThat(binder.getPoolType()).isEqualTo(PoolType.SIMPLE);
            assertThat(binder.getMaximumPoolSize()).isEqualTo(5);
            assertThat(ctx).hasSingleBean(BootJdbcProperties.class);
            BootJdbcProperties jdbcProps = ctx.getBean(BootJdbcProperties.class);
            assertThat(jdbcProps.isSqlLogEnabled()).isTrue();
            assertThat(jdbcProps.getSlowSqlThreshold()).isEqualTo(1000);
            assertThat(jdbcProps.getTenantColumn()).isEqualTo("tenant_id");
        });
    }

    @Test
    void shouldCreateAllCoreBeans() {
        context.run(ctx -> {
            assertThat(ctx).hasSingleBean(ConnectionProvider.class);
            assertThat(ctx).hasSingleBean(JdbcProperties.class);
            assertThat(ctx).hasSingleBean(DialectRegistry.class);
            assertThat(ctx).hasSingleBean(EntityMetadataResolver.class);
            assertThat(ctx).hasSingleBean(JdbcTemplate.class);
            assertThat(ctx).hasSingleBean(NamedParameterTemplate.class);
            assertThat(ctx).hasSingleBean(BatchTemplate.class);
            assertThat(ctx).hasSingleBean(SqlRunner.class);
            assertThat(ctx).hasSingleBean(RepositoryFactory.class);
            assertThat(ctx).hasSingleBean(TransactionManager.class);
        });
    }

    @Test
    void shouldResolveJdbcRepositoryFactoryBean() {
        context.run(ctx -> {
            RepositoryFactory factory = ctx.getBean(RepositoryFactory.class);
            assertThat(factory).isInstanceOf(JdbcRepositoryFactory.class);
        });
    }

    @Test
    void shouldExecuteSqlAndMapResults() {
        context.run(ctx -> {
            JdbcTemplate template = ctx.getBean(JdbcTemplate.class);
            template.execute("CREATE TABLE app_user (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50) NOT NULL, status INT DEFAULT 1)");
            int inserted = template.update("INSERT INTO app_user (name, status) VALUES (?, ?)", "alice", 1);
            assertThat(inserted).isEqualTo(1);
            List<Map<String, Object>> rows = template.queryForMaps("SELECT * FROM app_user");
            assertThat(rows).hasSize(1);
            assertThat(rows.getFirst().get("name")).isEqualTo("alice");
            assertThat(rows.getFirst().get("status")).isEqualTo(1);
        });
    }

    @Test
    void shouldSupportNamedParameters() {
        context.run(ctx -> {
            JdbcTemplate template = ctx.getBean(JdbcTemplate.class);
            template.execute("CREATE TABLE app_user2 (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50) NOT NULL)");
            NamedParameterTemplate named = ctx.getBean(NamedParameterTemplate.class);
            named.update("INSERT INTO app_user2 (name) VALUES (:name)", Map.of("name", "bob"));
            List<Map<String, Object>> rows = template.queryForMaps("SELECT name FROM app_user2 WHERE name = ?", "bob");
            assertThat(rows).hasSize(1);
            assertThat(rows.getFirst().get("name")).isEqualTo("bob");
        });
    }

    @Test
    void shouldCloseProviderOnContextClose() {
        context.run(ctx -> {
            ConnectionProvider provider = ctx.getBean(ConnectionProvider.class);
            assertThat(provider.isClosed()).isFalse();
        });
    }
}
