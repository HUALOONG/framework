package cn.jowen.framework.boot.autoconfigure.data;

import cn.jowen.framework.data.core.exception.ExceptionTranslator;
import cn.jowen.framework.data.mybatis.config.MybatisFlexProperties;
import cn.jowen.framework.data.mybatis.extension.ExtensionRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MybatisAutoConfiguration} 集成测试。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class MybatisAutoConfigurationTest {

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MybatisAutoConfiguration.class))
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
                    "framework.data.mybatis.maximum-pool-size=10",
                    "framework.data.mybatis.type-aliases-package=com.example.entity",
                    "framework.data.mybatis.audit-enabled=true",
                    "framework.data.mybatis.tenant-enabled=true",
                    "framework.data.mybatis.sql-audit-enabled=true",
                    "framework.data.mybatis.logic-delete-enabled=true",
                    "framework.data.mybatis.optimistic-lock-enabled=true"
            );

    @Test
    void shouldBindProperties() {
        context.run(ctx -> {
            BootMybatisFlexProperties properties = ctx.getBean(BootMybatisFlexProperties.class);
            assertThat(properties.getUrl()).isEqualTo("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
            assertThat(properties.getUsername()).isEqualTo("sa");
            assertThat(properties.getMaximumPoolSize()).isEqualTo(10);
            assertThat(properties.getMapperLocations()).isEqualTo("classpath*:/mapper/**/*Mapper.xml");
            assertThat(properties.getTypeAliasesPackage()).isEqualTo("com.example.entity");
            assertThat(properties.isAuditEnabled()).isTrue();
            assertThat(properties.isTenantEnabled()).isTrue();
            assertThat(properties.isSqlAuditEnabled()).isTrue();
            assertThat(properties.isLogicDeleteEnabled()).isTrue();
            assertThat(properties.isOptimisticLockEnabled()).isTrue();
            assertThat(properties.isEncryptEnabled()).isFalse();
        });
    }

    @Test
    void shouldCreateExtensionRegistryBean() {
        context.run(ctx -> {
            Object registry = ctx.getBean("extensionRegistry", Object.class);
            assertThat(registry).isNotNull();
            assertThat(registry).isInstanceOf(ExtensionRegistry.class);
        });
    }

    @Test
    void shouldCreateExceptionTranslatorBean() {
        context.run(ctx -> {
            assertThat(ctx).hasSingleBean(ExceptionTranslator.class);
            ExceptionTranslator translator = ctx.getBean("mybatisExceptionTranslator", ExceptionTranslator.class);
            assertThat(translator).isNotNull();
        });
    }

    @Test
    void shouldCreateMybatisFlexPropertiesBean() {
        context.run(ctx -> {
            assertThat(ctx).hasSingleBean(BootMybatisFlexProperties.class);
            BootMybatisFlexProperties props = ctx.getBean(BootMybatisFlexProperties.class);
            assertThat(props).isInstanceOf(MybatisFlexProperties.class);
        });
    }

    @Test
    void shouldNotCreateRepositoryFactoryWithoutSqlSessionFactory() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MybatisAutoConfiguration.class))
                .withPropertyValues("framework.data.mybatis.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean("flexRepositoryFactory");
                });
    }

    @Test
    void shouldNotActivateWhenDisabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MybatisAutoConfiguration.class))
                .withPropertyValues("framework.data.mybatis.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean("extensionRegistry");
                    assertThat(ctx).doesNotHaveBean("mybatisExceptionTranslator");
                    assertThat(ctx).doesNotHaveBean(BootMybatisFlexProperties.class);
                });
    }
}
