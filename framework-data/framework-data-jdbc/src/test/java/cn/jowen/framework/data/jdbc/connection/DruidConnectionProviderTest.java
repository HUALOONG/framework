package cn.jowen.framework.data.jdbc.connection;

import cn.jowen.framework.data.core.datasource.DataSourceProperties;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * {@link DruidConnectionProvider} 单元测试（H2 内存库）。
 *
 * <p>Druid 为 optional 依赖，若测试 classpath 缺失则跳过依赖真实数据源的用例；
 * 反射降级路径通过「数据源置空」与「隔离类加载器」覆盖。
 */
class DruidConnectionProviderTest {

    private static final String PROVIDER_CLASS =
            "cn.jowen.framework.data.jdbc.connection.DruidConnectionProvider";

    private static boolean druidAvailable() {
        try {
            Class.forName("com.alibaba.druid.pool.DruidDataSource");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private DataSourceProperties h2Properties() {
        DataSourceProperties properties = new DataSourceProperties();
        properties.setUrl("jdbc:h2:mem:druid_provider;DB_CLOSE_DELAY=-1");
        properties.setUsername("sa");
        properties.setPassword("");
        properties.setDriverClassName("org.h2.Driver");
        properties.setName("test");
        properties.setMaximumPoolSize(5);
        properties.setConnectionTimeout(3000);
        return properties;
    }

    /** 将内部数据源置空，使后续反射调用必定失败。 */
    private static void nullOutDataSource(DruidConnectionProvider provider) throws Exception {
        java.lang.reflect.Field field =
                DruidConnectionProvider.class.getDeclaredField("dataSource");
        field.setAccessible(true);
        field.set(provider, null);
    }

    @Test
    void getConnection_returnsConnection() throws Exception {
        Assumptions.assumeTrue(druidAvailable());
        DruidConnectionProvider provider = new DruidConnectionProvider(h2Properties());
        try (Connection connection = provider.getConnection()) {
            assertThat(connection).isNotNull();
        } finally {
            provider.close();
        }
    }

    @Test
    void close_thenIsClosed() {
        Assumptions.assumeTrue(druidAvailable());
        DruidConnectionProvider provider = new DruidConnectionProvider(h2Properties());
        assertThat(provider.isClosed()).isFalse();

        provider.close();

        assertThat(provider.isClosed()).isTrue();
    }

    @Test
    void getConnection_afterClose_throws() {
        Assumptions.assumeTrue(druidAvailable());
        DruidConnectionProvider provider = new DruidConnectionProvider(h2Properties());
        provider.close();

        assertThatThrownBy(provider::getConnection)
                .isInstanceOf(SQLException.class);
    }

    @Test
    void getConnection_dataSourceUnavailable_wrapsWithoutCause() throws Exception {
        Assumptions.assumeTrue(druidAvailable());
        DruidConnectionProvider provider = new DruidConnectionProvider(h2Properties());
        // 数据源不可用时反射失败且无 SQLException 成因，走「包装为 SQLException」分支
        nullOutDataSource(provider);

        assertThatThrownBy(provider::getConnection)
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("获取 Druid 连接失败");
    }

    @Test
    void close_dataSourceUnavailable_ignored() throws Exception {
        Assumptions.assumeTrue(druidAvailable());
        DruidConnectionProvider provider = new DruidConnectionProvider(h2Properties());
        nullOutDataSource(provider);

        // 反射失败被静默忽略，不向外抛出
        provider.close();
    }

    @Test
    void isClosed_dataSourceUnavailable_returnsFalse() throws Exception {
        Assumptions.assumeTrue(druidAvailable());
        DruidConnectionProvider provider = new DruidConnectionProvider(h2Properties());
        nullOutDataSource(provider);

        assertThat(provider.isClosed()).isFalse();
    }

    @Test
    void constructor_druidHidden_throwsIllegalStateException() throws Exception {
        Assumptions.assumeTrue(druidAvailable());
        // 在隔离类加载器中屏蔽 Druid，使 Class.forName 抛出 ClassNotFoundException
        ClassLoader loader = new DruidHidingClassLoader(getClass().getClassLoader());
        Class<?> providerClass = loader.loadClass(PROVIDER_CLASS);
        Constructor<?> constructor = providerClass.getDeclaredConstructor(DataSourceProperties.class);
        constructor.setAccessible(true);

        InvocationTargetException wrapper = catchThrowableOfType(
                () -> constructor.newInstance(h2Properties()),
                InvocationTargetException.class);

        assertThat(wrapper).as("构造函数应抛出 InvocationTargetException").isNotNull();
        assertThat(wrapper.getCause())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("druid");
    }

    /**
     * 屏蔽 {@code com.alibaba.druid} 的类加载器：仅自行定义 {@code DruidConnectionProvider}，
     * 其余类型（含 {@link DataSourceProperties}）委托父加载器，确保实参类型兼容。
     */
    private static final class DruidHidingClassLoader extends ClassLoader {

        private DruidHidingClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith("com.alibaba.druid")) {
                throw new ClassNotFoundException(name);
            }
            if (PROVIDER_CLASS.equals(name)) {
                synchronized (getClassLoadingLock(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null) {
                        loaded = defineProviderClass();
                    }
                    if (resolve) {
                        resolveClass(loaded);
                    }
                    return loaded;
                }
            }
            return super.loadClass(name, resolve);
        }

        private Class<?> defineProviderClass() throws ClassNotFoundException {
            String resource = PROVIDER_CLASS.replace('.', '/') + ".class";
            byte[] bytes;
            try (InputStream in = getParent().getResourceAsStream(resource)) {
                if (in == null) {
                    throw new ClassNotFoundException(PROVIDER_CLASS);
                }
                bytes = in.readAllBytes();
            } catch (IOException e) {
                throw new ClassNotFoundException(PROVIDER_CLASS, e);
            }
            return defineClass(PROVIDER_CLASS, bytes, 0, bytes.length);
        }
    }
}
