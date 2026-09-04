package cn.jowen.framework.boot.autoconfigure.data;

import cn.jowen.framework.boot.autoconfigure.data.MybatisAutoConfiguration.FlexMapperFactoryBean;
import cn.jowen.framework.boot.autoconfigure.data.MybatisAutoConfiguration.FlexMapperScanner;
import cn.jowen.framework.boot.autoconfigure.data.mapperfixture.MappedOrderMapper;
import cn.jowen.framework.data.mybatis.config.FlexGlobalConfigCustomizer;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.session.SqlSessionManager;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link MybatisAutoConfiguration} 缺口补齐测试。
 *
 * <p>既有 {@code MybatisAutoConfigurationTest} 走 {@code ApplicationContextRunner} 覆盖了条件装配与属性绑定，
 * 但 {@code @Bean} 方法体、内置 Mapper 扫描器与 {@code FactoryBean} 未触达。
 * 本类直接调用 {@code @Bean} 方法与包级内部类，绕开容器以精准命中方法体分支。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class MybatisAutoConfigurationGapTest {

    private static final String FIXTURE_PACKAGE =
            "cn.jowen.framework.boot.autoconfigure.data.mapperfixture";

    private static final String H2_URL = "jdbc:h2:mem:gap_ds;DB_CLOSE_DELAY=-1";

    /**
     * Hikari 会立即初始化连接池，必须与延迟连接的 MyBatis 内置连接池使用独立内存库，
     * 否则同一 JVM 内的同名 H2 库会因首个连接者的凭据已固定而报「Wrong user name or password」。
     */
    private static final String HIKARI_URL_A = "jdbc:h2:mem:gap_hikari_a;DB_CLOSE_DELAY=-1";

    private static final String HIKARI_URL_B = "jdbc:h2:mem:gap_hikari_b;DB_CLOSE_DELAY=-1";

    private final MybatisAutoConfiguration configuration = new MybatisAutoConfiguration();

    // ---- simpleDataSource ----

    @Test
    void simpleDataSource_withDriver_returnsPooledDataSource() {
        BootMybatisFlexProperties properties = new BootMybatisFlexProperties();
        properties.setDriverClassName("org.h2.Driver");
        properties.setUrl(H2_URL);
        properties.setUsername("sa");
        properties.setPassword("");

        assertThat(configuration.simpleDataSource(properties)).isNotNull();
    }

    @Test
    void simpleDataSource_withoutDriver_throwsIllegalState() {
        BootMybatisFlexProperties properties = new BootMybatisFlexProperties();
        properties.setUrl(H2_URL);

        assertThatThrownBy(() -> configuration.simpleDataSource(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("driver-class-name 未配置");
    }

    // ---- sqlSessionFactory ----

    @Test
    void sqlSessionFactory_multipleCustomizers_enablesAudit() {
        BootMybatisFlexProperties properties = new BootMybatisFlexProperties();
        properties.setAuditEnabled(true);

        @SuppressWarnings({"unchecked", "rawtypes"})
        ObjectProvider<FlexGlobalConfigCustomizer> customizers = stubCustomizers(
                config -> {
                },
                config -> {
                }
        );

        assertThat(configuration.sqlSessionFactory(h2DataSource(), properties, customizers, stubProvider()))
                .isNotNull();
    }

    @Test
    void sqlSessionFactory_blankMapperLocations_skipsXmlLoading() {
        BootMybatisFlexProperties properties = new BootMybatisFlexProperties();
        properties.setMapperLocations("   ");

        @SuppressWarnings({"unchecked", "rawtypes"})
        ObjectProvider<FlexGlobalConfigCustomizer> customizers = Mockito.mock(ObjectProvider.class);
        Mockito.when(customizers.orderedStream()).thenReturn(Stream.of());

        assertThat(configuration.sqlSessionFactory(h2DataSource(), properties, customizers, stubProvider()))
                .isNotNull();
    }

    @Test
    void sqlSessionFactory_withMapperLocations_splitsBlankSegments() {
        // 空白段由 loadMapperXmls 的 hasText 判断短路，不应产生异常
        BootMybatisFlexProperties properties = new BootMybatisFlexProperties();
        properties.setMapperLocations("classpath*:/mapper/**/*Mapper.xml, ,");

        @SuppressWarnings({"unchecked", "rawtypes"})
        ObjectProvider<FlexGlobalConfigCustomizer> customizers = Mockito.mock(ObjectProvider.class);
        Mockito.when(customizers.orderedStream()).thenReturn(Stream.of());

        assertThat(configuration.sqlSessionFactory(h2DataSource(), properties, customizers, stubProvider()))
                .isNotNull();
    }

    // ---- FlexMapperScanner ----

    @Test
    void flexMapperScanner_registersOnlyMapperInterfaces() {
        DefaultListableBeanFactory registry = new DefaultListableBeanFactory();

        new FlexMapperScanner(FIXTURE_PACKAGE)
                .postProcessBeanDefinitionRegistry(registry);

        assertThat(registry.containsBeanDefinition(FIXTURE_PACKAGE + ".MappedOrderMapper")).isTrue();
        assertThat(registry.containsBeanDefinition(FIXTURE_PACKAGE + ".BaseMapperEntityMapper")).isTrue();
        assertThat(registry.containsBeanDefinition(FIXTURE_PACKAGE + ".PlainInterface")).isFalse();
        assertThat(registry.containsBeanDefinition(FIXTURE_PACKAGE + ".NotAMapperClass")).isFalse();
    }

    @Test
    void flexMapperScanner_blankBasePackages_isNoOp() {
        DefaultListableBeanFactory registry = new DefaultListableBeanFactory();

        new FlexMapperScanner("   ").postProcessBeanDefinitionRegistry(registry);

        assertThat(registry.getBeanDefinitionCount()).isZero();
    }

    @Test
    void hikariDataSource_bindsDriverPoolAndTimeoutSettings() {
        BootMybatisFlexProperties properties = new BootMybatisFlexProperties();
        properties.setDriverClassName("org.h2.Driver");
        properties.setUrl(HIKARI_URL_A);
        properties.setUsername("sa");
        properties.setPassword("");
        properties.setMaximumPoolSize(4);
        properties.setConnectionTimeout(2000L);

        HikariDataSource dataSource =
                (HikariDataSource) configuration.hikariDataSource(properties);
        try {
            // HikariConfig 属性透传校验：驱动、连接串、池参数均来自配置
            assertThat(dataSource.getDriverClassName()).isEqualTo("org.h2.Driver");
            assertThat(dataSource.getJdbcUrl()).isEqualTo(HIKARI_URL_A);
            assertThat(dataSource.getMaximumPoolSize()).isEqualTo(4);
            assertThat(dataSource.getConnectionTimeout()).isEqualTo(2000L);
        } finally {
            dataSource.close();
        }
    }

    @Test
    void hikariDataSource_withoutDriver_omitsDriverClassBinding() {
        BootMybatisFlexProperties properties = new BootMybatisFlexProperties();
        properties.setUrl(HIKARI_URL_B);

        HikariDataSource dataSource =
                (HikariDataSource) configuration.hikariDataSource(properties);
        try {
            // 未指定驱动类名时不绑定 driverClassName，交由 JDBC SPI 按 jdbcUrl 推断
            assertThat(dataSource.getJdbcUrl()).isEqualTo(HIKARI_URL_B);
            assertThat(dataSource.getUsername()).isEqualTo("");
        } finally {
            dataSource.close();
        }
    }

    // ---- FlexMapperFactoryBean ----

    @Test
    void flexMapperFactoryBean_resolvesExistingMapperType() {
        String mapperClass = FIXTURE_PACKAGE + ".MappedOrderMapper";
        FlexMapperFactoryBean<Object> factoryBean =
                new FlexMapperFactoryBean<>(mapperClass, Mockito.mock(SqlSessionManager.class));

        assertThat(factoryBean.getObjectType())
                .isSameAs(MappedOrderMapper.class);
        // mock 默认返回 null，验证工厂仅做类型解析与委派，不吞掉返回值
        assertThat(factoryBean.getObject()).isNull();
        assertThat(factoryBean.isSingleton()).isTrue();
    }

    @Test
    void flexMapperFactoryBean_unknownType_returnsNullAndWrapsLookupFailure() {
        FlexMapperFactoryBean<Object> factoryBean =
                new FlexMapperFactoryBean<>("com.does.not.Exist", Mockito.mock(SqlSessionManager.class));

        assertThat(factoryBean.getObjectType()).isNull();
        assertThatThrownBy(factoryBean::getObject)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Mapper 接口不存在");
    }

    // ---- helpers ----

    @SafeVarargs
    private static ObjectProvider<FlexGlobalConfigCustomizer> stubCustomizers(
            FlexGlobalConfigCustomizer... customizers) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        ObjectProvider<FlexGlobalConfigCustomizer> provider = Mockito.mock(ObjectProvider.class);
        Mockito.when(provider.orderedStream()).thenReturn(Stream.of(customizers));
        return provider;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ObjectProvider stubProvider() {
        return Mockito.mock(ObjectProvider.class);
    }

    private static javax.sql.DataSource h2DataSource() {
        return new PooledDataSource("org.h2.Driver", H2_URL, "sa", "");
    }
}
