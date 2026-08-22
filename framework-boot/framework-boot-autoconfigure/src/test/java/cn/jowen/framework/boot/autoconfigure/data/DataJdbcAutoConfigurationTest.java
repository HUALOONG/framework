package cn.jowen.framework.boot.autoconfigure.data;

import cn.jowen.framework.data.core.meta.Column;
import cn.jowen.framework.data.core.meta.Id;
import cn.jowen.framework.data.core.meta.Table;
import cn.jowen.framework.data.core.repository.Repository;
import cn.jowen.framework.data.core.repository.RepositoryFactory;
import cn.jowen.framework.data.core.transaction.TransactionManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JDBC 数据装配集成测试：验证空配置下框架 JDBC 能力自动装配生效，并能跑通基础 CRUD。
 */
class DataJdbcAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    DataJdbcAutoConfiguration.class))
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:framework_test;DB_CLOSE_DELAY=-1",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=");

    @Test
    void jdbcAutoConfigurationRegistersBeans() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(RepositoryFactory.class);
            assertThat(context).hasSingleBean(TransactionManager.class);
        });
    }

    @Test
    void repositoryCrudWorks() {
        runner.run(context -> {
            javax.sql.DataSource ds = context.getBean(javax.sql.DataSource.class);
            try (java.sql.Connection c = ds.getConnection();
                 java.sql.Statement st = c.createStatement()) {
                st.execute("CREATE TABLE t_user (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50))");
            }

            RepositoryFactory factory = context.getBean(RepositoryFactory.class);
            Repository<User, Long> repo = factory.getRepository(User.class);

            User saved = repo.save(new User(null, "alice"));
            assertThat(saved.getId()).isNotNull();

            Optional<User> found = repo.findById(saved.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("alice");

            repo.deleteById(saved.getId());
            assertThat(repo.findById(saved.getId())).isEmpty();
        });
    }

    @Test
    void jdbcDisabledByProperty() {
        runner.withPropertyValues("framework.data.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(RepositoryFactory.class);
        });
    }

    @Table("t_user")
    static final class User {
        @Id
        private @Column("id") Long id;
        @Column("name")
        private String name;

        User(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        /** 供框架 {@code BeanRowMapper} 反射实例化使用。 */
        User() {
        }

        Long getId() {
            return id;
        }

        String getName() {
            return name;
        }
    }
}
