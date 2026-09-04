package cn.jowen.framework.plugin.loader;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link FrameworkApiDelegateClassLoader} 无父加载器时的系统类加载器兜底契约。
 *
 * <p>父加载器为 {@code null} 是双亲委派断裂的边界形态，若此处直接 NPE，
 * 插件将在"找不到类"的场景下抛出误导性异常，掩盖真实原因。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class FrameworkApiDelegateClassLoaderFallbackTest {

    @Test
    void constructor_nullParent_yieldsNullParent() {
        FrameworkApiDelegateClassLoader loader =
                new FrameworkApiDelegateClassLoader(new URL[0], null, List.of());

        assertThat(loader.getParent()).isNull();
    }

    @Test
    void loadClass_nullParentFallsBackToSystemClassLoader() {
        FrameworkApiDelegateClassLoader loader =
                new FrameworkApiDelegateClassLoader(new URL[0], null, List.of());

        assertThatThrownBy(() -> loader.loadClass("com.nonexistent.UnlikelyPluginClass"))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
