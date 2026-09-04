package cn.jowen.framework.logger.facade;

import cn.jowen.framework.logger.adapter.LoggerAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link LoggerFactory} 测试。
 */
class LoggerFactoryTest {

    @BeforeEach
    void setUp() {
        // 重置适配器，确保测试隔离
        LoggerFactory.setAdapter(null);
    }

    @AfterEach
    void tearDown() {
        LoggerFactory.setAdapter(null);
    }

    @Test
    void getLogger_byClass_returnsLogger() {
        TestAdapter adapter = new TestAdapter();
        LoggerFactory.setAdapter(adapter);
        Logger logger = LoggerFactory.getLogger(TestLogger.class);
        assertThat(logger).isNotNull();
        assertThat(logger).isInstanceOf(Logger.class);
    }

    @Test
    void getLogger_byName_returnsLogger() {
        TestAdapter adapter = new TestAdapter();
        LoggerFactory.setAdapter(adapter);
        Logger logger = LoggerFactory.getLogger("test.logger.name");
        assertThat(logger).isNotNull();
    }

    @Test
    void getLogger_nameNotNull() {
        TestAdapter adapter = new TestAdapter();
        LoggerFactory.setAdapter(adapter);
        Logger logger = LoggerFactory.getLogger("myLogger");
        assertThat(logger).isNotNull();
    }

    @Test
    void resolveAdapter_withoutSetAdapter_resolvesViaSpiAndCaches() {
        // setUp 已将 adapter 置空，此处不预设，强制走 DCL 慢路径经 ExtensionLoader 解析
        // （原先此处用 try-catch 双重通过，两种结果均算成功，属无断言空壳测试）
        LoggerAdapter resolved = LoggerFactory.resolveAdapter();
        assertThat(resolved).isNotNull();
        // 经 SPI 解析出的适配器必须真正可用，而非空实现
        assertThat(resolved.getLogger("spi.resolved.logger")).isNotNull();
        // 解析结果应被缓存，二次调用命中缓存返回同一实例
        assertThat(LoggerFactory.resolveAdapter()).isSameAs(resolved);
    }

    @Test
    void setAdapter_andGetLogger() {
        TestAdapter adapter = new TestAdapter();
        LoggerFactory.setAdapter(adapter);
        Logger logger = LoggerFactory.getLogger("test");
        assertThat(logger).isNotNull();
        // 验证使用的是我们设置的 adapter
        assertThat(adapter.getLoggerCalled).isTrue();
        assertThat(adapter.lastGetName()).isEqualTo("test");
    }

    @Test
    void resolveAdapter_cachesResult() {
        TestAdapter adapter = new TestAdapter();
        LoggerFactory.setAdapter(adapter);
        LoggerAdapter resolved1 = LoggerFactory.resolveAdapter();
        LoggerAdapter resolved2 = LoggerFactory.resolveAdapter();
        assertThat(resolved1).isSameAs(resolved2);
        assertThat(resolved1).isSameAs(adapter);
    }

    @Test
    void getLogger_multipleCalls_sameAdapter() {
        TestAdapter adapter = new TestAdapter();
        LoggerFactory.setAdapter(adapter);
        Logger logger1 = LoggerFactory.getLogger("a");
        Logger logger2 = LoggerFactory.getLogger("b");
        assertThat(logger1).isNotNull();
        assertThat(logger2).isNotNull();
    }

    /**
     * 测试用的 LoggerAdapter 实现。
     */
    private static class TestAdapter implements LoggerAdapter {
        volatile boolean getLoggerCalled = false;
        volatile String lastName;

        @Override
        public Logger getLogger(String name) {
            getLoggerCalled = true;
            lastName = name;
            return new TestLogger();
        }

        String lastGetName() {
            return lastName;
        }
    }

    /**
     * 测试用的 Logger 实现。
     */
    private static class TestLogger implements Logger {
        @Override
        public void trace(String msg, Object... args) {
        }

        @Override
        public void debug(String msg, Object... args) {
        }

        @Override
        public void info(String msg, Object... args) {
        }

        @Override
        public void warn(String msg, Object... args) {
        }

        @Override
        public void error(String msg, Object... args) {
        }

        @Override
        public void error(String msg, Throwable t, Object... args) {
        }

        @Override
        public boolean isEnabled(LogLevel level) {
            return false;
        }
    }
}
