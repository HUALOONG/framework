package cn.jowen.framework.plugin.spi;

import cn.jowen.framework.core.spi.NamedExtension;
import cn.jowen.framework.plugin.registry.Extension;
import cn.jowen.framework.plugin.registry.ExtensionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link PluginExtensionSource} 测试。
 */
class PluginExtensionSourceTest {

    interface Point {
        String id();
    }

    static final class ImplA implements Point {
        @Override
        public String id() {
            return "impl-a";
        }
    }

    static final class ImplB implements Point {
        @Override
        public String id() {
            return "impl-b";
        }
    }

    /** 不属于 Point 类型，用于类型不符跳过校验。 */
    static final class WrongType {
    }

    private ExtensionRegistry registry;
    private PluginExtensionSource<Point> source;

    @BeforeEach
    void setUp() {
        registry = new ExtensionRegistry();
        source = new PluginExtensionSource<>(registry, "point-a", Point.class);
    }

    @Test
    void load_mapsRegistryExtensions() {
        registry.register(new Extension("impl-a", "point-a", new ImplA(), 10, "p1", null));
        registry.register(new Extension("impl-b", "point-a", new ImplB(), 5, "p1", null));

        List<NamedExtension<Point>> loaded = source.load(Point.class, getClass().getClassLoader());
        assertThat(loaded).hasSize(2);
        // registry 按 order 升序，load 保持该顺序
        assertThat(loaded.getFirst().name()).isEqualTo("impl-b");
        NamedExtension<Point> b = loaded.getFirst();
        assertThat(b.instance()).isInstanceOf(ImplB.class);
        assertThat(b.order()).isEqualTo(5);
        assertThat(b.activated()).isTrue();
        assertThat(b.groups()).isEmpty();
        assertThat(b.sourceId()).isEqualTo("bridge:point-a");
    }

    @Test
    void load_skipsTypeMismatch() {
        registry.register(new Extension("bad", "point-a", new WrongType(), 0, "p1", null));
        registry.register(new Extension("good", "point-a", new ImplA(), 0, "p1", null));

        List<NamedExtension<Point>> loaded = source.load(Point.class, getClass().getClassLoader());
        assertThat(loaded).extracting(NamedExtension::name).containsExactly("good");
    }

    @Test
    void load_otherExtensionPoint_ignored() {
        registry.register(new Extension("other", "point-b", new ImplA(), 0, "p1", null));
        assertThat(source.load(Point.class, getClass().getClassLoader())).isEmpty();
    }

    @Test
    void addChangeListener_forwardsToRegistry() {
        AtomicInteger fired = new AtomicInteger();
        source.addChangeListener(fired::incrementAndGet);

        registry.register(new Extension("a", "point-a", new ImplA(), 0, "p1", null));
        assertThat(fired.get()).isEqualTo(1);

        registry.unregister("point-a", "a");
        assertThat(fired.get()).isEqualTo(2);
    }

    @Test
    void nullArguments_throw() {
        assertThatThrownBy(() -> new PluginExtensionSource<>(null, "x", Point.class))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PluginExtensionSource<>(registry, " ", Point.class))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PluginExtensionSource<>(registry, "x", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}