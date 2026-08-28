package cn.jowen.framework.extras.storage.strategy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ObjectNames} 文件名提取与扩展名解析验证。
 *
 * <p>该类为包级私有工具，测试与实现同包以便直接调用静态方法。
 */
class ObjectNamesTest {

    // ---------- fileNameOf ----------

    @Test
    void fileNameOfPlainName() {
        assertThat(ObjectNames.fileNameOf("photo.jpg")).isEqualTo("photo.jpg");
    }

    @Test
    void fileNameOfStripsUnixPath() {
        assertThat(ObjectNames.fileNameOf("a/b/c.txt")).isEqualTo("c.txt");
    }

    @Test
    void fileNameOfStripsWindowsPath() {
        assertThat(ObjectNames.fileNameOf("a\\b\\c.txt")).isEqualTo("c.txt");
    }

    @Test
    void fileNameOfNormalizesMixedSlashes() {
        assertThat(ObjectNames.fileNameOf("a/b\\c.png")).isEqualTo("c.png");
    }

    @Test
    void fileNameOfBlankYieldsUnnamed() {
        assertThat(ObjectNames.fileNameOf("")).isEqualTo("unnamed");
        assertThat(ObjectNames.fileNameOf("   ")).isEqualTo("unnamed");
    }

    @Test
    void fileNameOfNullYieldsUnnamed() {
        assertThat(ObjectNames.fileNameOf(null)).isEqualTo("unnamed");
    }

    @Test
    void fileNameOfTrailingSlashYieldsUnnamed() {
        assertThat(ObjectNames.fileNameOf("dir/")).isEqualTo("unnamed");
    }

    // ---------- extensionOf ----------

    @Test
    void extensionOfWithSingleDot() {
        assertThat(ObjectNames.extensionOf("photo.jpg")).isEqualTo(".jpg");
    }

    @Test
    void extensionOfWithMultipleDotsTakesLast() {
        assertThat(ObjectNames.extensionOf("archive.tar.gz")).isEqualTo(".gz");
    }

    @Test
    void extensionOfNoDotReturnsEmpty() {
        assertThat(ObjectNames.extensionOf("README")).isEmpty();
    }

    @Test
    void extensionOfLeadingDotReturnsEmpty() {
        // ".gitignore" 的 lastIndexOf('.') == 0，约定无扩展名
        assertThat(ObjectNames.extensionOf(".gitignore")).isEmpty();
    }

    @Test
    void extensionOfBlankReturnsEmpty() {
        assertThat(ObjectNames.extensionOf("")).isEmpty();
    }
}
