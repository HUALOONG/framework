package cn.jowen.framework.plugin.context;

import cn.jowen.framework.plugin.descriptor.PluginDescriptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

/**
 * {@link DefaultPluginContext#close()} 对 Spring 上下文关闭失败的容错契约。
 *
 * <p>容器侧关闭失败不应阻断插件卸载流程——卸载必须尽力完成并置为停止态，
 * 否则一次容器异常会让插件永久卡在加载态，只能重启进程恢复。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class DefaultPluginContextCloseFailureTest {

    @Test
    void close_springContextCloseThrows_logsWarningAndContinues() {
        AutoCloseable throwingContext = () -> {
            throw new IllegalStateException("容器关闭失败");
        };

        DefaultPluginContext ctx = new DefaultPluginContext(
                "demo",
                new PluginDescriptor("demo"),
                mock(PluginConfiguration.class),
                new SharedData(),
                null,
                getClass().getClassLoader(),
                getClass().getClassLoader(),
                throwingContext);

        assertThatCode(ctx::close).doesNotThrowAnyException();
    }
}
