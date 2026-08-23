package cn.jowen.framework.core.spi;

import cn.jowen.framework.core.exception.SystemException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

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

    @SPI
    interface TestSPI {
        void doWork();
    }

    @SPI
    interface NoDefaultSPI {
        void run();
    }
}