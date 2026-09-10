package cn.jowen.framework.core.spi;

import cn.jowen.framework.core.exception.SystemException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ExtensionLoader} 多源聚合与动态源行为测试。
 *
 * <p>采用 {@link Nested} 隔离策略：每个内部测试类持有独立的加载器基线，
 * 静态实现（{@link StaticFilter}、{@link ActivatedFilter}）通过 sourceId 为 {@code "static"}
 * 的 {@link MockSource} 注入，而非依赖 {@code META-INF/services} 静态注册。
 * 动态源由每个测试类自己的 {@link #initSource()} 创建，在 {@link #cleanup()} 中移除，
 * 避免污染 JVM 级共享的加载器单例。
 *
 * <p>这种设计消除了跨测试类的状态泄漏（如 {@code sourceChange_invalidatesCaches} 在
 * 全量运行时偶发失败），因为每个 {@code @Nested} 类都有独立的 {@code Filter} 接口实例与
 * 加载器缓存。
 */
class ExtensionLoaderMultiSourceTest {

    @SPI(value = "static-filter")
    interface Filter {
        String id();
    }

    @SPIImplementation(name = "static-filter", order = 10)
    public static final class StaticFilter implements Filter {
        @Override
        public String id() {
            return "static-filter";
        }
    }

    @SPIImplementation(name = "activated-filter")
    @Activate(order = -10)
    public static final class ActivatedFilter implements Filter {
        @Override
        public String id() {
            return "activated-filter";
        }
    }

    static final class DynamicFilter implements Filter {
        private final String id;

        DynamicFilter(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }
    }

    /**
     * 可变测试源：候选内容可变，可主动触发变更回调。
     */
    static final class MockSource<T> implements ExtensionSource<T> {
        private final String id;
        private final List<NamedExtension<T>> candidates = new CopyOnWriteArrayList<>();
        private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

        MockSource(String id, List<NamedExtension<T>> initial) {
            this.id = id;
            this.candidates.addAll(initial);
        }

        @Override
        public String sourceId() {
            return id;
        }

        @Override
        public List<NamedExtension<T>> load(Class<T> extensionPoint, ClassLoader classLoader) {
            return List.copyOf(candidates);
        }

        @Override
        public void addChangeListener(Runnable listener) {
            listeners.add(listener);
        }

        void fire() {
            listeners.forEach(Runnable::run);
        }

        void add(NamedExtension<T> ext) {
            candidates.add(ext);
        }
    }

    /** 静态基线源：包含 StaticFilter 与 ActivatedFilter，sourceId 固定为 "static"。 */
    private static final ExtensionSource<Filter> STATIC_SOURCE = new MockSource<>("static", List.of(
            new NamedExtension<>("static-filter", new StaticFilter(), 10, new String[0], false, "static"),
            new NamedExtension<>("activated-filter", new ActivatedFilter(), -10, new String[0], true, "static")
    ));

    private final List<String> addedSources = new ArrayList<>();

    /**
     * 为每个 {@code @Nested} 类创建独立的加载器：先注册静态基线源，确保每个嵌套类都能拿到
     * {@code StaticFilter} 与 {@code ActivatedFilter}，但不依赖 {@code META-INF/services}。
     */
    void initSource() {
        ExtensionLoader<Filter> loader = ExtensionLoader.getExtensionLoader(Filter.class);
        loader.addSource(STATIC_SOURCE);
        addedSources.add(STATIC_SOURCE.sourceId());
    }

    @org.junit.jupiter.api.AfterEach
    void cleanup() {
        ExtensionLoader<Filter> loader = ExtensionLoader.getExtensionLoader(Filter.class);
        for (String sourceId : addedSources) {
            loader.removeSource(sourceId);
        }
        addedSources.clear();
    }

    private void addMockSource(MockSource<Filter> source) {
        ExtensionLoader.getExtensionLoader(Filter.class).addSource(source);
        addedSources.add(source.sourceId());
    }

    // -------------------------------------------------------------------------
    // 静态源基础行为
    // -------------------------------------------------------------------------

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class StaticSourceBehaviors {

        @org.junit.jupiter.api.BeforeEach
        void setUp() {
            initSource();
        }

        @Test
        void staticSource_findsServiceLoaderImplementations() {
            ExtensionLoader<Filter> loader = ExtensionLoader.getExtensionLoader(Filter.class);
            assertThat(loader.getExtension("static-filter")).isInstanceOf(StaticFilter.class);
            assertThat(loader.getDefaultExtension()).isInstanceOf(StaticFilter.class);
            assertThat(loader.getExtensionsBySource(STATIC_SOURCE.sourceId())).hasSize(2);
        }

        @Test
        void staticSource_emptyGroupActivate_stillActivated() {
            // 防回归：@Activate 空分组必须算作激活（logger 模块适配器依赖该语义）
            ExtensionLoader<Filter> loader = ExtensionLoader.getExtensionLoader(Filter.class);
            List<String> names = loader.getActivateExtensions().stream()
                    .map(f -> f.getClass().getSimpleName())
                    .toList();
            assertThat(names).containsExactly("ActivatedFilter");
        }
    }

    // -------------------------------------------------------------------------
    // 动态源聚合与覆盖
    // -------------------------------------------------------------------------

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class DynamicSourceMerging {

        @org.junit.jupiter.api.BeforeEach
        void setUp() {
            initSource();
        }

        @Test
        void addSource_mergesDynamicImplementations() {
            MockSource<Filter> source = new MockSource<>("plugin:p1", List.of(
                    new NamedExtension<>("dynamic-filter", new DynamicFilter("dynamic-filter"), 5,
                            new String[0], true, "plugin:p1")));
            addMockSource(source);

            ExtensionLoader<Filter> loader = ExtensionLoader.getExtensionLoader(Filter.class);
            assertThat(loader.getExtension("dynamic-filter")).isInstanceOf(DynamicFilter.class);
            assertThat(loader.getAllExtensions()).extracting(Filter::id)
                    .contains("static-filter", "dynamic-filter");
        }

        @Test
        void laterSource_overridesSameName() {
            MockSource<Filter> source = new MockSource<>("plugin:p1", List.of(
                    new NamedExtension<>("static-filter", new DynamicFilter("override"), 5,
                            new String[0], true, "plugin:p1")));
            addMockSource(source);

            ExtensionLoader<Filter> loader = ExtensionLoader.getExtensionLoader(Filter.class);
            assertThat(loader.getExtension("static-filter")).isInstanceOf(DynamicFilter.class);
        }
    }

    // -------------------------------------------------------------------------
    // 动态源移除
    // -------------------------------------------------------------------------

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class SourceRemoval {

        @org.junit.jupiter.api.BeforeEach
        void setUp() {
            initSource();
        }

        @Test
        void removeSource_cleansUpOnUnload() {
            MockSource<Filter> source = new MockSource<>("plugin:p1", List.of(
                    new NamedExtension<>("removable", new DynamicFilter("removable"), 5,
                            new String[0], true, "plugin:p1")));
            addMockSource(source);

            ExtensionLoader<Filter> loader = ExtensionLoader.getExtensionLoader(Filter.class);
            assertThat(loader.getExtension("removable")).isNotNull();

            assertThat(loader.removeSource("plugin:p1")).isTrue();
            addedSources.remove("plugin:p1");
            assertThatThrownBy(() -> loader.getExtension("removable"))
                    .isInstanceOf(SystemException.class)
                    .hasMessageContaining("未找到 SPI 实现");
            assertThat(loader.removeSource("plugin:p1")).isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // 缓存失效
    // -------------------------------------------------------------------------

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class CacheInvalidation {

        @org.junit.jupiter.api.BeforeEach
        void setUp() {
            initSource();
        }

        @Test
        void sourceChange_invalidatesCaches() {
            MockSource<Filter> source = new MockSource<>("plugin:p1", List.of());
            addMockSource(source);

            ExtensionLoader<Filter> loader = ExtensionLoader.getExtensionLoader(Filter.class);
            List<String> before = loader.getActivateExtensions().stream()
                    .map(f -> f.getClass().getSimpleName())
                    .toList();
            assertThat(before).doesNotContain("DynamicFilter");

            // 预热 namedCache 后再变更
            assertThatThrownBy(() -> loader.getExtension("hot-filter")).isInstanceOf(SystemException.class);

            source.add(new NamedExtension<>("hot-filter", new DynamicFilter("hot-filter"), 1,
                    new String[0], true, "plugin:p1"));
            source.fire();

            assertThat(loader.getExtension("hot-filter")).isInstanceOf(DynamicFilter.class);
            assertThat(loader.getActivateExtensions()).extracting(f -> f.getClass().getSimpleName())
                    .contains("DynamicFilter");
        }
    }

    // -------------------------------------------------------------------------
    // 激活顺序与分组
    // -------------------------------------------------------------------------

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class ActivateOrderAndGroups {

        @org.junit.jupiter.api.BeforeEach
        void setUp() {
            initSource();
        }

        @Test
        void dynamicSource_activateOrderAndGroups() {
            MockSource<Filter> source = new MockSource<>("plugin:p1", List.of(
                    new NamedExtension<>("g2-1", new DynamicFilter("g2-1"), 20,
                            new String[]{"g2"}, true, "plugin:p1"),
                    new NamedExtension<>("g1-1", new DynamicFilter("g1-1"), 10,
                            new String[]{"g1"}, true, "plugin:p1"),
                    new NamedExtension<>("plain", new DynamicFilter("plain"), 5,
                            new String[0], false, "plugin:p1")));
            addMockSource(source);

            ExtensionLoader<Filter> loader = ExtensionLoader.getExtensionLoader(Filter.class);
            List<DynamicFilter> all = loader.getActivateExtensions().stream()
                    .filter(DynamicFilter.class::isInstance)
                    .map(f -> (DynamicFilter) f)
                    .toList();
            // 未激活候选不进入集合；按 order 升序
            assertThat(all).extracting(DynamicFilter::id).containsExactly("g1-1", "g2-1");

            List<DynamicFilter> g1 = loader.getActivateExtensions("g1").stream()
                    .filter(DynamicFilter.class::isInstance)
                    .map(f -> (DynamicFilter) f)
                    .toList();
            assertThat(g1).extracting(DynamicFilter::id).containsExactly("g1-1");
        }
    }

    // -------------------------------------------------------------------------
    // 参数校验
    // -------------------------------------------------------------------------

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class NullValidation {

        @org.junit.jupiter.api.BeforeEach
        void setUp() {
            initSource();
        }

        @Test
        void addSource_null_throws() {
            ExtensionLoader<Filter> loader = ExtensionLoader.getExtensionLoader(Filter.class);
            assertThatThrownBy(() -> loader.addSource(null))
                    .isInstanceOf(SystemException.class)
                    .hasMessageContaining("扩展源不能为 null");
        }
    }

    // -------------------------------------------------------------------------
    // 并发安全
    // -------------------------------------------------------------------------

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class ConcurrentSafety {

        @org.junit.jupiter.api.BeforeEach
        void setUp() {
            initSource();
        }

        @Test
        void concurrentAddQueryRemove_safe() throws Exception {
            ExtensionLoader<Filter> loader = ExtensionLoader.getExtensionLoader(Filter.class);
            int tasks = 8;
            ExecutorService pool = Executors.newFixedThreadPool(tasks);
            try {
                List<Future<String>> futures = new ArrayList<>();
                for (int i = 0; i < tasks; i++) {
                    int idx = i;
                    futures.add(pool.submit(() -> {
                        String sourceId = "plugin:p" + idx;
                        String name = "ext-" + idx;
                        MockSource<Filter> source = new MockSource<>(sourceId, List.of(
                                new NamedExtension<>(name, new DynamicFilter(name), idx,
                                        new String[0], true, sourceId)));
                        loader.addSource(source);
                        boolean found = loader.getExtension(name) != null;
                        boolean removed = loader.removeSource(sourceId);
                        return found + "/" + removed;
                    }));
                }
                for (Future<String> f : futures) {
                    assertThat(f.get(10, TimeUnit.SECONDS)).isEqualTo("true/true");
                }
                // 全部移除后恢复静态基线，同名扩展不再可见
                assertThatThrownBy(() -> loader.getExtension("ext-0"))
                        .isInstanceOf(SystemException.class);
            } finally {
                pool.shutdownNow();
            }
        }
    }
}
