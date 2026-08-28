package cn.jowen.framework.extras.storage.strategy;

import cn.jowen.framework.extras.properties.NamingStrategy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link OriginalNameStrategy} 边界与契约验证（补充 factory 测试未覆盖的用例）。
 */
class OriginalNameStrategyTest {

    private final OriginalNameStrategy strategy = new OriginalNameStrategy();

    @Test
    void keepsPlainFileName() {
        assertThat(strategy.generate("photo.jpg", null)).isEqualTo("photo.jpg");
    }

    @Test
    void stripsUnixDirectory() {
        assertThat(strategy.generate("uploads/2024/photo.jpg", null)).isEqualTo("photo.jpg");
    }

    @Test
    void stripsWindowsDirectory() {
        assertThat(strategy.generate("a\\b\\c.txt", null)).isEqualTo("c.txt");
    }

    @Test
    void mixedSlashesNormalized() {
        assertThat(strategy.generate("a/b\\c.png", null)).isEqualTo("c.png");
    }

    @Test
    void blankNameFallsBackToUnnamed() {
        assertThat(strategy.generate("", null)).isEqualTo("unnamed");
        assertThat(strategy.generate("   ", null)).isEqualTo("unnamed");
        assertThat(strategy.generate(null, null)).isEqualTo("unnamed");
    }

    @Test
    void trailingSlashYieldsUnnamed() {
        assertThat(strategy.generate("dir/", null)).isEqualTo("unnamed");
    }

    @Test
    void preservesExtension() {
        assertThat(strategy.generate("archive.tar.gz", null)).isEqualTo("archive.tar.gz");
    }

    @Test
    void typeIsOriginal() {
        assertThat(strategy.type()).isEqualTo(NamingStrategy.ORIGINAL);
    }
}
