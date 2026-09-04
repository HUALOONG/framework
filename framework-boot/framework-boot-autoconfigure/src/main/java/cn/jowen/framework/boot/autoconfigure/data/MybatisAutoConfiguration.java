package cn.jowen.framework.boot.autoconfigure.data;

import cn.jowen.framework.boot.autoconfigure.JowenAutoConfiguration;
import cn.jowen.framework.data.core.exception.ExceptionTranslator;
import cn.jowen.framework.data.mybatis.adapter.FlexExceptionTranslator;
import cn.jowen.framework.data.mybatis.config.FlexGlobalConfigCustomizer;
import cn.jowen.framework.data.mybatis.extension.ExtensionRegistry;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.audit.AuditManager;
import com.mybatisflex.core.mybatis.FlexConfiguration;
import com.mybatisflex.core.mybatis.FlexSqlSessionFactoryBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionManager;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Primary;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * MyBatis Flex 数据层自动装配（M2）。
 *
 * <p>通过 {@code framework.data.mybatis.enabled=true} 显式启用（与 {@code framework.data.datasource.*} JDBC 模式二选一）。装配流程：
 * <ol>
 *     <li>绑定 {@code framework.data.mybatis.*} 属性（数据源 + MyBatis Flex 专属配置）；</li>
 *     <li>构建 {@link javax.sql.DataSource}（优先 HikariCP，缺省回退 MyBatis 内置连接池）；</li>
 *     <li>以 {@link FlexConfiguration} + {@link FlexSqlSessionFactoryBuilder} 装配
 *         {@link SqlSessionFactory}，注册类型别名、加载 Mapper XML、扫描并注册 Flex 基础 Mapper；</li>
 *     <li>向 Spring 容器暴露 {@link SqlSessionManager}、{@link ExtensionRegistry}、
 *         {@link ExceptionTranslator} 等组件；业务 Mapper 继承 {@link BaseMapper} 直接使用 Flex 能力；</li>
 *     <li>扫描 {@code framework.data.mybatis.type-aliases-package} 下的 {@code @Mapper} 或继承
 *         {@link BaseMapper} 的接口，注册为 Spring Bean，支持 {@code @Autowired} 直接注入。</li>
 * </ol>
 *
 * <p>典型配置：
 * <pre>{@code
 * framework:
 *   data:
 *     mybatis:
 *       enabled: true
 *       url: jdbc:mysql://localhost:3306/app
 *       username: root
 *       password: root
 *       maximum-pool-size: 20
 *       connection-timeout: 5000
 *       type-aliases-package: com.example.app.entity
 * }</pre>
 *
 * <p>关闭方式：{@code framework.data.mybatis.enabled=false}。
 *
 * <p>说明：多租户 / 逻辑删除 / 乐观锁 / 脱敏 / 字段加密等扩展通过 {@link ExtensionRegistry}
 * 与 MyBatis Flex 全局配置生效，业务 Mapper 直接继承 {@link BaseMapper} 即可使用全部能力；
 * 业务方可通过 {@link FlexGlobalConfigCustomizer} Bean 定制全局配置。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@AutoConfiguration(after = JowenAutoConfiguration.class)
@ConditionalOnClass(name = {
        "com.mybatisflex.core.FlexGlobalConfig",
        "com.mybatisflex.core.mybatis.FlexConfiguration",
        "org.apache.ibatis.session.SqlSessionFactory"
})
@ConditionalOnProperty(prefix = "framework.data.mybatis", name = "enabled", matchIfMissing = true)
public class MybatisAutoConfiguration {

    private static final String FLEX_SQL_SESSION_MANAGER_BEAN = "sqlSessionManager";

    /**
     * MyBatis Flex 属性绑定 Bean，通过 {@link Binder} 手动绑定 {@code framework.data.mybatis.*} 属性。
     *
     * @param env 环境变量
     * @return 已绑定属性的实例
     */
    @Bean("mybatisFlexProperties")
    public BootMybatisFlexProperties mybatisFlexProperties(org.springframework.core.env.Environment env) {
        return Binder.get(env).bind("framework.data.mybatis",
                        Bindable.of(BootMybatisFlexProperties.class))
                .orElseGet(BootMybatisFlexProperties::new);
    }

    /**
     * HikariCP 数据源（缺省优先）。未显式提供 {@link javax.sql.DataSource} 且 HikariCP 在类路径时生效。
     * 要求 {@code framework.data.mybatis.url} 已配置。
     *
     * @param properties MyBatis Flex 属性
     * @return Hikari 数据源
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnClass(name = "com.zaxxer.hikari.HikariDataSource")
    @ConditionalOnMissingBean(javax.sql.DataSource.class)
    @Conditional(DataSourceUrlCondition.class)
    public javax.sql.DataSource hikariDataSource(BootMybatisFlexProperties properties) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.getUrl());
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());
        if (StringUtils.hasText(properties.getDriverClassName())) {
            config.setDriverClassName(properties.getDriverClassName());
        }
        config.setMaximumPoolSize(properties.getMaximumPoolSize());
        config.setConnectionTimeout(properties.getConnectionTimeout());
        return new HikariDataSource(config);
    }

    /**
     * MyBatis 内置连接池兜底：无 HikariCP 且未提供 {@link javax.sql.DataSource} 时生效。
     * 要求 {@code framework.data.mybatis.url} 与 {@code framework.data.mybatis.driver-class-name} 均已配置。
     *
     * @param properties MyBatis Flex 属性
     * @return MyBatis 连接池数据源
     */
    @Bean(destroyMethod = "")
    @ConditionalOnMissingBean(javax.sql.DataSource.class)
    @Conditional(DataSourceUrlCondition.class)
    public javax.sql.DataSource simpleDataSource(BootMybatisFlexProperties properties) {
        String driver = properties.getDriverClassName();
        if (!StringUtils.hasText(driver)) {
            throw new IllegalStateException(
                    "framework.data.mybatis.driver-class-name 未配置，MyBatis 内置连接池无法推断驱动，请显式指定驱动类名（如 com.mysql.cj.jdbc.Driver）");
        }
        return new PooledDataSource(driver, properties.getUrl(),
                properties.getUsername(), properties.getPassword());
    }

    /**
     * 装配 MyBatis Flex 会话工厂：类型别名、Mapper XML、Flex 基础 Mapper 注册与全局配置定制。
     *
     * @param dataSource         数据源
     * @param properties         MyBatis Flex 属性
     * @param customizers        全局配置定制回调
     * @param extensionRegistry  扩展注册中心（可选，供注册路径使用）
     * @return 会话工厂
     */
    @Bean
    @ConditionalOnMissingBean
    @Conditional(DataSourceUrlCondition.class)
    public SqlSessionFactory sqlSessionFactory(javax.sql.DataSource dataSource,
                                               BootMybatisFlexProperties properties,
                                               ObjectProvider<FlexGlobalConfigCustomizer> customizers,
                                               ObjectProvider<ExtensionRegistry> extensionRegistry) {
        Environment environment = new Environment("flex", new JdbcTransactionFactory(), dataSource);
        FlexConfiguration configuration = new FlexConfiguration(environment);

        registerTypeAliases(configuration, properties.getTypeAliasesPackage());
        loadMapperXmls(configuration, properties.getMapperLocations());
        registerFlexMappers(configuration, properties.getTypeAliasesPackage());

        SqlSessionFactory sqlSessionFactory = new FlexSqlSessionFactoryBuilder().build(configuration);

        FlexGlobalConfig globalConfig = FlexGlobalConfig.getConfig(configuration);
        globalConfig.setSqlSessionFactory(sqlSessionFactory);
        for (FlexGlobalConfigCustomizer customizer : customizers.orderedStream().toList()) {
            customizer.customize(globalConfig);
        }

        if (properties.isAuditEnabled() || properties.isSqlAuditEnabled()) {
            AuditManager.setAuditEnable(true);
        }
        return sqlSessionFactory;
    }

    /**
     * 线程安全的会话管理器，提供 {@code getMapper(Class)} 能力（供仓储工厂与 Mapper Bean 使用）。
     *
     * @param sqlSessionFactory 会话工厂
     * @return 会话管理器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(SqlSessionFactory.class)
    public SqlSessionManager sqlSessionManager(SqlSessionFactory sqlSessionFactory) {
        return SqlSessionManager.newInstance(sqlSessionFactory);
    }

    /**
     * 框架扩展注册中心（多租户 / 脱敏 / 加密 / SQL 审计 / 乐观锁 / 逻辑删除）。
     *
     * @return 扩展注册中心
     */
    @Bean("extensionRegistry")
    @ConditionalOnMissingBean
    public ExtensionRegistry extensionRegistry() {
        return ExtensionRegistry.defaults();
    }

    /**
     * MyBatis 异常翻译器：将 MyBatis / MyBatis Flex 异常转换为框架统一异常体系。
     *
     * @return 异常翻译器
     */
    @Bean("mybatisExceptionTranslator")
    @ConditionalOnMissingBean
    public ExceptionTranslator mybatisExceptionTranslator() {
        return FlexExceptionTranslator.getInstance();
    }

    /**
     * Mapper 扫描器：将 {@code framework.data.mybatis.type-aliases-package} 下标注 {@code @Mapper}
     * 或继承 {@link BaseMapper} 的接口注册为 Spring Bean（{@code @Autowired} 可注入）。
     *
     * @param properties MyBatis Flex 属性
     * @return Mapper 扫描器
     */
    @Bean
    public static FlexMapperScanner flexMapperScanner(BootMybatisFlexProperties properties) {
        return new FlexMapperScanner(properties.getTypeAliasesPackage());
    }

    private static void registerTypeAliases(FlexConfiguration configuration, String packages) {
        for (String pkg : splitPackages(packages)) {
            configuration.getTypeAliasRegistry().registerAliases(pkg);
        }
    }

    private static void loadMapperXmls(FlexConfiguration configuration, String mapperLocations) {
        if (!StringUtils.hasText(mapperLocations)) {
            return;
        }
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        for (String location : mapperLocations.split(",")) {
            if (!StringUtils.hasText(location)) {
                continue;
            }
            try {
                for (Resource resource : resolver.getResources(location.trim())) {
                    try (InputStream in = resource.getInputStream()) {
                        XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(
                                in, configuration, resource.toString(), configuration.getSqlFragments());
                        xmlMapperBuilder.parse();
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("加载 Mapper XML 失败: " + location, e);
            }
        }
    }

    private static void registerFlexMappers(FlexConfiguration configuration, String packages) {
        for (String pkg : splitPackages(packages)) {
            configuration.addMappers(pkg, BaseMapper.class);
        }
    }

    private static List<String> splitPackages(@Nullable String packages) {
        List<String> result = new ArrayList<>(4);
        if (!StringUtils.hasText(packages)) {
            return result;
        }
        for (String part : packages.split("[,;\\s]+")) {
            if (StringUtils.hasText(part)) {
                result.add(part.trim());
            }
        }
        return result;
    }

    /**
     * 自定义条件：仅当 {@code framework.data.mybatis.url} 有非空值时创建 DataSource Bean。
     */
    static final class DataSourceUrlCondition extends SpringBootCondition {

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String url = context.getEnvironment().getProperty("framework.data.mybatis.url");
            if (StringUtils.hasText(url)) {
                return ConditionOutcome.match("framework.data.mybatis.url is configured: " + url);
            }
            return ConditionOutcome.noMatch("framework.data.mybatis.url is not configured or empty");
        }
    }

    /**
     * Mapper 接口扫描注册处理器。
     */
    @NullMarked
    static class FlexMapperScanner implements BeanDefinitionRegistryPostProcessor {

        private final String basePackages;

        FlexMapperScanner(String basePackages) {
            this.basePackages = basePackages;
        }

        @Override
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
            if (!StringUtils.hasText(basePackages)) {
                return;
            }
            ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
                /**
                 * 覆盖默认候选判定：MyBatis Mapper 均为接口，而父类默认拒绝非具体类，
                 * 与本扫描器的包含过滤器（仅接口）互斥，会导致任何 Mapper 都扫描不到。
                 */
                @Override
                protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                    return beanDefinition.getMetadata().isIndependent();
                }
            };
            scanner.addIncludeFilter((metadataReader, metadataReaderFactory) -> {
                AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
                if (!metadata.isInterface()) {
                    return false;
                }
                if (metadata.hasAnnotation(Mapper.class.getName())) {
                    return true;
                }
                for (String interfaceName : metadata.getInterfaceNames()) {
                    if (BaseMapper.class.getName().equals(interfaceName)) {
                        return true;
                    }
                }
                return false;
            });
            for (String pkg : splitPackages(basePackages)) {
                for (BeanDefinition candidate : scanner.findCandidateComponents(pkg)) {
                    String className = candidate.getBeanClassName();
                    if (className == null || registry.containsBeanDefinition(className)) {
                        continue;
                    }
                    BeanDefinition definition = BeanDefinitionBuilder
                            .genericBeanDefinition(FlexMapperFactoryBean.class)
                            .addConstructorArgValue(className)
                            .addConstructorArgReference(FLEX_SQL_SESSION_MANAGER_BEAN)
                            .getBeanDefinition();
                    registry.registerBeanDefinition(className, definition);
                }
            }
        }

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            // no-op
        }
    }

    /**
     * Mapper 接口的 Spring Bean 工厂：通过 {@link SqlSessionManager} 获取代理实例。
     *
     * @param <T> Mapper 接口类型
     */
    @NullMarked
    static class FlexMapperFactoryBean<T> implements FactoryBean<T> {

        private final String mapperInterface;
        private final SqlSessionManager sessionManager;

        FlexMapperFactoryBean(String mapperInterface, SqlSessionManager sessionManager) {
            this.mapperInterface = mapperInterface;
            this.sessionManager = sessionManager;
        }

        @SuppressWarnings("unchecked")
        @Override
        public T getObject() {
            try {
                return (T) sessionManager.getMapper(Class.forName(mapperInterface));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Mapper 接口不存在: " + mapperInterface, e);
            }
        }

        @Override
        public @Nullable Class<?> getObjectType() {
            try {
                return Class.forName(mapperInterface);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        @Override
        public boolean isSingleton() {
            return FactoryBean.super.isSingleton();
        }
    }
}
