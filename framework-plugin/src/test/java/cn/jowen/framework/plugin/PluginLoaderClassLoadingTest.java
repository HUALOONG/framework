package cn.jowen.framework.plugin;

import cn.jowen.framework.plugin.classloader.FrameworkApiDelegateClassLoader;
import cn.jowen.framework.plugin.PluginDescriptor;
import pluginimpl.StubPlugin;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link PluginLoader} 接入三层 {@link cn.jowen.framework.plugin.classloader.PluginClassLoader} 体系的验证。
 *
 * <p>桩插件 {@code pluginimpl.StubPlugin} 故意置于非 {@code cn.jowen.framework} 包下，使其可被插件内部分类加载器
 * 自身定义；其引用的框架类（{@code Plugin}）经由 delegate 委派父加载器，与宿主共享同一 {@code Class}。
 */
class PluginLoaderClassLoadingTest {

    private static final URL[] NO_URLS = new URL[0];
    private static final URL TEST_CLASSES =
            StubPlugin.class.getProtectionDomain().getCodeSource().getLocation();

    /** AC1：delegate 下加载引用框架类的插件成功，且插件内部解析到的框架类与宿主同一对象。 */
    @Test
    void delegateLoadsFrameworkReferencingPluginAndSharesFrameworkClass() throws Exception {
        ClassLoader parent = StubPlugin.class.getClassLoader();
        PluginLoader loader = new PluginLoader(descriptorFor("pluginimpl.StubPlugin", "stub"),
                new URL[]{TEST_CLASSES}, parent);

        Plugin plugin = loader.load();
        assertThat(plugin).isNotNull();

        // 插件类由内部 FrameworkApiDelegateClassLoader 定义
        assertThat(plugin.getClass().getClassLoader()).isInstanceOf(FrameworkApiDelegateClassLoader.class);
        // 插件引用的框架类经 delegate 委派父加载器，与宿主 Plugin.class 同一对象
        Class<?> frameworkPluginClass =
                plugin.getClass().getClassLoader().getParent().loadClass("cn.jowen.framework.plugin.Plugin");
        assertThat(frameworkPluginClass).isSameAs(Plugin.class);
    }

    /** AC2a：delegate 下插件私有类由自身类加载器解析，框架类委派父加载器。 */
    @Test
    void delegatePrivateClassResolvedByOwnClassLoaderFrameworkDelegatedToParent() throws Exception {
        ClassLoader parent = StubPlugin.class.getClassLoader();
        PluginLoader loader = new PluginLoader(descriptorFor("pluginimpl.StubPlugin", "stub"),
                new URL[]{TEST_CLASSES}, parent);

        Plugin plugin = loader.load();
        // 私有类由自身分类加载器解析（defining loader 即内部类加载器）
        assertThat(plugin.getClass().getClassLoader()).isInstanceOf(FrameworkApiDelegateClassLoader.class);
        // 框架类仍走父委派，与宿主同一对象
        assertThat(parent.loadClass("cn.jowen.framework.plugin.Plugin")).isSameAs(Plugin.class);
    }

    /** AC2b：两个 PluginLoader 各自持有独立类加载器实例，同名私有类互不干扰。 */
    @Test
    void twoLoadersHoldIndependentClassLoaderInstances() throws Exception {
        ClassLoader parent = StubPlugin.class.getClassLoader();
        PluginLoader loaderA = new PluginLoader(descriptorFor("pluginimpl.StubPlugin", "a"),
                new URL[]{TEST_CLASSES}, parent);
        PluginLoader loaderB = new PluginLoader(descriptorFor("pluginimpl.StubPlugin", "b"),
                new URL[]{TEST_CLASSES}, parent);

        Plugin pluginA = loaderA.load();
        Plugin pluginB = loaderB.load();

        // 同名私有类由各自独立的分类加载器定义 → 不同 Class 对象，互不干扰
        assertThat(pluginA.getClass()).isNotSameAs(pluginB.getClass());
        assertThat(pluginA.getClass().getClassLoader()).isNotSameAs(pluginB.getClass().getClassLoader());
    }

    /** AC2c：isolated 策略不委派父加载器，加载 classpath 上的框架类应失败。 */
    @Test
    void isolatedDoesNotDelegateToParentForFrameworkClass() {
        ClassLoader parent = StubPlugin.class.getClassLoader();
        PluginLoader loader = new PluginLoader(descriptorFor("cn.jowen.framework.plugin.Plugin", "fw"),
                NO_URLS, parent, "isolated");

        // load() 内部把 ClassNotFoundException 包装为 PluginException（既有契约不变），故断言包装后的异常
        assertThatThrownBy(loader::load)
                .isInstanceOf(PluginLoader.PluginException.class)
                .hasCauseInstanceOf(ClassNotFoundException.class);
    }

    /** T3 回归：getResource / close 转发到内部类加载器。 */
    @Test
    void resourceAndCloseForwardToInternalClassLoader() throws Exception {
        ClassLoader parent = StubPlugin.class.getClassLoader();
        PluginLoader loader = new PluginLoader(descriptorFor("pluginimpl.StubPlugin", "stub"),
                new URL[]{TEST_CLASSES}, parent);

        // delegate 下框架资源经父委派可定位
        assertThat(loader.getResource("cn/jowen/framework/plugin/Plugin.class")).isNotNull();
        loader.close();
    }

    private static PluginDescriptor descriptorFor(String className, String id) {
        return new PluginDescriptor(id, "1.0.0", className, "", java.util.List.of());
    }
}
