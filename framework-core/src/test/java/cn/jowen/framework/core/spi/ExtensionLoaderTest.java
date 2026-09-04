package cn.jowen.framework.core.spi;

import cn.jowen.framework.core.exception.SystemException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * {@link ExtensionLoader} 测试。
 */
class ExtensionLoaderTest {

    @Test
    void getExtensionLoader_notInterface_throws() {
        assertThatThrownBy(() -> ExtensionLoader.getExtensionLoader(String.class))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("必须是接口");
    }

    @Test
    void getExtensionLoader_sameLoaderReturned() {
        ExtensionLoader<TestSPI> l1 = ExtensionLoader.getExtensionLoader(TestSPI.class);
        ExtensionLoader<TestSPI> l2 = ExtensionLoader.getExtensionLoader(TestSPI.class);
        assertThat(l1).isSameAs(l2);
    }

    @Test
    void getExtension_notFound_throws() {
        ExtensionLoader<TestSPI> loader = ExtensionLoader.getExtensionLoader(TestSPI.class);
        assertThatThrownBy(() -> loader.getExtension("nonexistent"))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("未找到 SPI 实现");
    }

    @Test
    void getExtension_blankName_throws() {
        ExtensionLoader<TestSPI> loader = ExtensionLoader.getExtensionLoader(TestSPI.class);
        assertThatThrownBy(() -> loader.getExtension("  "))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("扩展名不能为空");
    }

    @Test
    void getDefaultExtension_noDefault_throws() {
        ExtensionLoader<NoDefaultSPI> loader = ExtensionLoader.getExtensionLoader(NoDefaultSPI.class);
        assertThatThrownBy(loader::getDefaultExtension)
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("未声明默认实现");
    }

    @Test
    void getActivateExtensions_emptyList() {
        ExtensionLoader<TestSPI> loader = ExtensionLoader.getExtensionLoader(TestSPI.class);
        assertThat(loader.getActivateExtensions()).isEmpty();
    }

    @Test
    void removeSource_null_throws() {
        ExtensionLoader<TestSPI> loader = ExtensionLoader.getExtensionLoader(TestSPI.class);
        assertThatThrownBy(() -> loader.removeSource(null))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("sourceId 不能为 null");
    }

    @Test
    void getExtensionsBySource_null_throws() {
        ExtensionLoader<TestSPI> loader = ExtensionLoader.getExtensionLoader(TestSPI.class);
        assertThatThrownBy(() -> loader.getExtensionsBySource(null))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("sourceId 不能为 null");
    }

    @Test
    void refreshDynamicSources_doesNotThrow() {
        ExtensionLoader<TestSPI> loader = ExtensionLoader.getExtensionLoader(TestSPI.class);
        assertThatCode(loader::refreshDynamicSources).doesNotThrowAnyException();
    }

    @Test
    void addSource_sourceWithoutOverride_usesDefaultNoopListener() {
        // 未重写 addChangeListener 的源走接口默认空实现（静态源内容不可变），注册不得抛异常
        ExtensionLoader<TestSPI> loader = ExtensionLoader.getExtensionLoader(TestSPI.class);
        ExtensionSource<TestSPI> staticSource = new ExtensionSource<>() {
            @Override
            public String sourceId() {
                return "test-noop-listener-source";
            }

            @Override
            public List<NamedExtension<TestSPI>> load(Class<TestSPI> extensionPoint, ClassLoader classLoader) {
                return List.of();
            }
        };

        assertThatCode(() -> loader.addSource(staticSource)).doesNotThrowAnyException();
    }

    @SPI
    interface TestSPI {
        void doWork();
    }

    @SPI
    interface NoDefaultSPI {
        void run();
    }
}