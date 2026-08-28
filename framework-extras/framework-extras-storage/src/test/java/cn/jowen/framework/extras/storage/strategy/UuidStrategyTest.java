package cn.jowen.framework.extras.storage.strategy;

import cn.jowen.framework.extras.properties.NamingStrategy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link UuidStrategy} 格式与唯一性验证（补充 factory 测试未覆盖的用例）。
 */
class UuidStrategyTest {

    private final UuidStrategy strategy = new UuidStrategy();

    @Test
    void generatesUuidWithExtension() {
        String key = strategy.generate("photo.jpg", null);
        assertThat(key).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.jpg");
    }

    @Test
    void noExtensionYieldsNoTrailingDot() {
        String key = strategy.generate("README", null);
        assertThat(key).doesNotEndWith(".");
        assertThat(key).matches("[0-9a-f-]{36}");
    }

    @Test
    void multipleDotsKeepsLastExtension() {
        String key = strategy.generate("archive.tar.gz", null);
        assertThat(key).endsWith(".gz");
    }

    @Test
    void generatedKeysAreUniqueAcrossCalls() {
        String a = strategy.generate("a.jpg", null);
        String b = strategy.generate("a.jpg", null);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void typeIsUuid() {
        assertThat(strategy.type()).isEqualTo(NamingStrategy.UUID);
    }
}
