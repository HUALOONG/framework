package cn.jowen.framework.plugin;

import cn.jowen.framework.plugin.dependency.DependencyResolutionException;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link DefaultPluginManager#loadAll(List)} 的行为验证：拓扑排序、循环依赖拒绝、
 * 批次内未满足依赖拒绝，以及单插件 {@code load} 的向后兼容。
 */
class PluginManagerLoadAllTest {

    private static final URL[] NO_URLS = new URL[0];

    private final DefaultPluginManager manager = new DefaultPluginManager();

    /** AC1：无环批量加载按拓扑序（依赖方在前）加载，返回值与 all() 顺序一致。 */
    @Test
    void loadAllOrdersByDependencyTopology() {
        // 输入顺序故意与拓扑序相反：c -> b -> a
        PluginLoader loaderC = loaderOf("c", PluginC.class, List.of("b"));
        PluginLoader loaderB = loaderOf("b", PluginB.class, List.of("a"));
        PluginLoader loaderA = loaderOf("a", PluginA.class, List.of());

        List<Plugin> loaded = manager.loadAll(List.of(loaderC, loaderB, loaderA));

        List<String> returnedIds = idsOf(loaded);
        assertThat(returnedIds.indexOf("a")).isLessThan(returnedIds.indexOf("b"));
        assertThat(returnedIds.indexOf("b")).isLessThan(returnedIds.indexOf("c"));
        assertThat(returnedIds).containsExactly("a", "b", "c");

        List<String> registeredIds = idsOf(manager.all());
        assertThat(registeredIds.indexOf("a")).isLessThan(registeredIds.indexOf("b"));
        assertThat(registeredIds.indexOf("b")).isLessThan(registeredIds.indexOf("c"));
        assertThat(registeredIds).containsExactly("a", "b", "c");

        // 每个插件均已启动，且返回的实例就是注册表内的实例
        for (Plugin plugin : loaded) {
            assertThat(((BasePlugin) plugin).started).isTrue();
            assertThat(manager.get(plugin.id())).isSameAs(plugin);
        }
    }

    /** AC1 补充：返回值不可修改，空批次返回空列表。 */
    @Test
    void loadAllReturnsUnmodifiableListAndSupportsEmptyBatch() {
        assertThat(manager.loadAll(List.of())).isEmpty();

        List<Plugin> loaded = manager.loadAll(List.of(loaderOf("a", PluginA.class, List.of())));
        assertThatThrownBy(() -> loaded.add(new PluginB()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    /** AC2：循环依赖整批拒绝，且不留下任何已注册插件。 */
    @Test
    void loadAllRejectsCircularDependency() {
        PluginLoader loaderA = loaderOf("a", PluginA.class, List.of("b"));
        PluginLoader loaderB = loaderOf("b", PluginB.class, List.of("a"));

        assertThatThrownBy(() -> manager.loadAll(List.of(loaderA, loaderB)))
                .isInstanceOf(DependencyResolutionException.class)
                .hasMessageContaining("循环依赖");

        assertThat(manager.all()).isEmpty();
        assertThat(manager.get("a")).isNull();
        assertThat(manager.get("b")).isNull();
    }

    /** AC4：依赖不在本批次内时整批拒绝，且不留下任何已注册插件。 */
    @Test
    void loadAllRejectsDependencyMissingFromBatch() {
        PluginLoader loaderA = loaderOf("a", PluginA.class, List.of());
        PluginLoader loaderB = loaderOf("b", PluginB.class, List.of("absent-plugin"));

        assertThatThrownBy(() -> manager.loadAll(List.of(loaderA, loaderB)))
                .isInstanceOf(DependencyResolutionException.class)
                .hasMessageContaining("未满足依赖")
                .hasMessageContaining("absent-plugin");

        // 解析先行，先于任何加载动作失败：连无依赖的 a 也不应被注册
        assertThat(manager.all()).isEmpty();
        assertThat(manager.get("a")).isNull();
    }

    /** AC3：单插件 load 的既有行为不变——无依赖插件正常加载并启动。 */
    @Test
    void singleLoadStillRegistersPluginWithoutDependencies() {
        manager.load(loaderOf("a", PluginA.class, List.of()));

        Plugin plugin = manager.get("a");
        assertThat(plugin).isNotNull();
        assertThat(((BasePlugin) plugin).started).isTrue();
        assertThat(idsOf(manager.all())).containsExactly("a");
    }

    /** AC3：单插件 load 在依赖未注册时仍抛 PluginException（不被 loadAll 改写）。 */
    @Test
    void singleLoadStillThrowsPluginExceptionWhenDependencyNotRegistered() {
        PluginLoader loaderB = loaderOf("b", PluginB.class, List.of("a"));

        assertThatThrownBy(() -> manager.load(loaderB))
                .isInstanceOf(PluginLoader.PluginException.class)
                .hasMessageContaining("依赖未满足");

        assertThat(manager.all()).isEmpty();
    }

    /** AC3：单插件 load 的依赖可由此前注册满足（与 loadAll 的批次自包含语义并存）。 */
    @Test
    void singleLoadAcceptsDependencyRegisteredEarlier() {
        manager.load(loaderOf("a", PluginA.class, List.of()));
        manager.load(loaderOf("b", PluginB.class, List.of("a")));

        assertThat(idsOf(manager.all())).containsExactly("a", "b");
    }

    /**
     * 边界（团队主理人点名核查）：批次内出现重复 id（两个 loader 同 id "a"）。
     * 验证不会误触 {@code loadAll} 中的防御分支（order 含 loaderById 没有的 id 时抛
     * DependencyResolutionException）——该分支在此场景下不应被触发；同时记录当前语义：
     * {@code loaderById} 为 {@link java.util.LinkedHashMap}，后加入的 loader 覆盖先加入者，
     * 因此 2 个同 id 的 loader 只会注册出 1 个插件，先加入的 loader 被静默丢弃
     * （与单插件 {@code load} 遇重复 id 抛「插件 id 已存在」的语义不一致，属本周期范围外
     * 的潜在隐患，已在报告中挂账）。
     */
    @Test
    void loadAllWithDuplicateIdsSilentlyDeduplicates() {
        // 两个 loader 同 id "a"（真实场景：两份声明同一 id 的插件）
        PluginLoader first = loaderOf("a", PluginA.class, List.of());
        PluginLoader second = loaderOf("a", PluginA.class, List.of());

        List<Plugin> loaded = manager.loadAll(List.of(first, second));

        // 防御分支（order 含 loaderById 没有的 id）未被误触，整批仍完成、不抛 DependencyResolutionException
        assertThat(loaded).hasSize(1);
        // 但 loaderById 为 LinkedHashMap，后加入的覆盖先加入者 —— 第一个 loader 被静默丢弃，
        // 2 个同 id loader 只注册出 1 个插件（id 为 "a"），与单 load 的重复 id 报错语义不一致
        assertThat(idsOf(manager.all())).containsExactly("a");
        assertThat(manager.get("a")).isNotNull();
    }

    /**
     * 构造一个以测试 classpath 为父加载器的插件加载器，插件类由父加载器解析。
     *
     * @param id           插件 id
     * @param impl         插件实现类
     * @param dependencies 依赖的插件 id 列表
     * @return 插件加载器
     */
    private static PluginLoader loaderOf(String id, Class<? extends Plugin> impl,
                                         List<String> dependencies) {
        PluginDescriptor descriptor =
                new PluginDescriptor(id, "1.0.0", impl.getName(), "", dependencies);
        return new PluginLoader(descriptor, NO_URLS,
                PluginManagerLoadAllTest.class.getClassLoader());
    }

    private static List<String> idsOf(List<Plugin> plugins) {
        List<String> ids = new ArrayList<>(plugins.size());
        for (Plugin plugin : plugins) {
            ids.add(plugin.id());
        }
        return ids;
    }

    /** 测试插件基类：记录启动/停止标志。 */
    abstract static class BasePlugin implements Plugin {

        boolean started = false;
        boolean stopped = false;

        @Override
        public String version() {
            return "1.0.0";
        }

        @Override
        public void afterPropertiesSet() {
            started = true;
        }

        @Override
        public void destroy() {
            stopped = true;
        }
    }

    /** id 为 {@code a} 的测试插件，需可被反射无参实例化。 */
    public static final class PluginA extends BasePlugin {

        public PluginA() {
            // 反射实例化所需的无参构造器
        }

        @Override
        public String id() {
            return "a";
        }
    }

    /** id 为 {@code b} 的测试插件，需可被反射无参实例化。 */
    public static final class PluginB extends BasePlugin {

        public PluginB() {
            // 反射实例化所需的无参构造器
        }

        @Override
        public String id() {
            return "b";
        }
    }

    /** id 为 {@code c} 的测试插件，需可被反射无参实例化。 */
    public static final class PluginC extends BasePlugin {

        public PluginC() {
            // 反射实例化所需的无参构造器
        }

        @Override
        public String id() {
            return "c";
        }
    }
}
