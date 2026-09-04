package cn.jowen.framework.plugin.registry;

import com.example.demo.DemoPlugin;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExtensionRegistryTest {

    private final ExtensionRegistry registry = new ExtensionRegistry();

    private Extension ext(String id, String point, int order, String pluginId) {
        return new Extension(id, point, new Object(), order, pluginId, null);
    }

    @Test
    void register_sortsByOrder() {
        Extension a = ext("a", "ep1", 2, "p1");
        Extension b = ext("b", "ep1", 1, "p2");
        registry.register(a);
        registry.register(b);
        assertThat(registry.getExtensions("ep1")).containsExactly(b, a);
    }

    @Test
    void register_null_throws() {
        assertThatThrownBy(() -> registry.register(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("extension cannot be null");
    }

    @Test
    void register_duplicateId_throws() {
        registry.register(ext("a", "ep1", 0, "p1"));
        assertThatThrownBy(() -> registry.register(ext("a", "ep1", 0, "p1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("扩展实现 id 重复");
    }

    @Test
    void unregister_missingReturnsNull_presentRemoved() {
        registry.register(ext("a", "ep1", 0, "p1"));
        assertThat(registry.unregister("ep1", "missing")).isNull();
        assertThat(registry.unregister("ep1", "a")).isNotNull();
        assertThat(registry.getExtensions("ep1")).isEmpty();
    }

    @Test
    void getExtensions_emptyPoint_returnsEmpty() {
        assertThat(registry.getExtensions("nope")).isEmpty();
    }

    @Test
    void getExtensionPointIds() {
        registry.register(ext("a", "ep1", 0, "p1"));
        registry.register(ext("b", "ep2", 0, "p1"));
        assertThat(registry.getExtensionPointIds()).containsExactlyInAnyOrder("ep1", "ep2");
    }

    @Test
    void clear_removesAll() {
        registry.register(ext("a", "ep1", 0, "p1"));
        registry.clear();
        assertThat(registry.getExtensionPointIds()).isEmpty();
    }

    @Test
    void unregisterPlugin_removesByPluginId() {
        registry.register(ext("a", "ep1", 0, "plug1"));
        registry.register(ext("b", "ep1", 0, "plug2"));
        assertThat(registry.unregisterPlugin("plug1")).isEqualTo(1);
        assertThat(registry.getExtensions("ep1")).hasSize(1);
        assertThatThrownBy(() -> registry.unregisterPlugin(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pluginId cannot be null");
    }

    @Test
    void getExtensionsByType_jdkInterface_returnsEmpty() {
        // Runnable 由启动类加载器加载（classloader 为 null），匹配分支跳过，返回空
        registry.register(ext("r", "epR", 0, "p1"));
        assertThat(registry.getExtensionsByType(Runnable.class)).isEmpty();
    }

    @Test
    void addChangeListener_null_throws() {
        assertThatThrownBy(() -> registry.addChangeListener(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("listener cannot be null");
    }

    @Test
    void changeListener_invokedAndExceptionTolerated() {
        List<String> fired = new ArrayList<>();
        registry.addChangeListener(() -> fired.add("changed"));
        registry.register(ext("a", "ep1", 0, "p1"));
        assertThat(fired).containsExactly("changed");

        // 监听抛出异常不影响注册表自身操作（notifyChanged 吞掉异常）
        registry.addChangeListener(() -> { throw new RuntimeException("boom"); });
        registry.register(ext("b", "ep1", 1, "p2"));
        // 第二次注册再次触发首个监听；抛异常监听被吞掉
        assertThat(fired).containsExactly("changed", "changed");
    }

    @Test
    void getExtensionsByType_appLoadedInterface_matches() {
        // Plugin 由应用类加载器加载（classloader 非 null），匹配分支被覆盖
        registry.register(new Extension("a", "ep1", new DemoPlugin(), 0, "p1", null));
        List<cn.jowen.framework.plugin.api.Plugin> result = registry.getExtensionsByType(cn.jowen.framework.plugin.api.Plugin.class);
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isInstanceOf(DemoPlugin.class);
    }

    @Test
    void getExtensionsByType_multipleMatches_invokesComparator() {
        // 两个匹配扩展触发排序比较器（lambda 体 return 0 被执行）
        registry.register(new Extension("a", "ep1", new DemoPlugin(), 0, "p1", null));
        registry.register(new Extension("b", "ep1", new DemoPlugin(), 0, "p2", null));
        List<cn.jowen.framework.plugin.api.Plugin> result = registry.getExtensionsByType(cn.jowen.framework.plugin.api.Plugin.class);
        assertThat(result).hasSize(2);
    }

    @Test
    void getExtensionsByType_arrayComponentClass_loadClassThrows_isSwallowed() {
        // 数组类的 classloader 非 null，但 loadClass(数组描述符) 抛 ClassNotFoundException，被桥接吞掉
        registry.register(ext("a", "ep1", 0, "p1"));
        assertThat(registry.getExtensionsByType(DemoPlugin[].class)).isEmpty();
    }
}
