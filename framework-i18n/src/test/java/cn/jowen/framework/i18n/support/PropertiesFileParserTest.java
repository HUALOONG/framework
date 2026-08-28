package cn.jowen.framework.i18n.support;

import cn.jowen.framework.i18n.api.ResourceLoadException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link PropertiesFileParser} 测试。
 */
class PropertiesFileParserTest {

    @TempDir
    Path tempDir;

    @Test
    void loadFromClasspath_found() {
        Properties props = PropertiesFileParser.loadFromClasspath("i18n/messages.properties");
        assertThat(props.getProperty("greeting")).isEqualTo("Hello");
    }

    @Test
    void loadFromClasspath_missing_returnsEmpty() {
        Properties props = PropertiesFileParser.loadFromClasspath("i18n/not-exist.properties");
        assertThat(props).isEmpty();
    }

    @Test
    void loadFromFile_found() throws IOException {
        Path file = tempDir.resolve("msg.properties");
        Files.writeString(file, "greeting=你好\n", StandardCharsets.UTF_8);

        Properties props = PropertiesFileParser.loadFromFile(file);
        assertThat(props.getProperty("greeting")).isEqualTo("你好");
    }

    @Test
    void loadFromFile_missing_returnsEmpty() {
        Properties props = PropertiesFileParser.loadFromFile(tempDir.resolve("nope.properties"));
        assertThat(props).isEmpty();
    }

    @Test
    void load_classpathWithSuffix() {
        Properties props = PropertiesFileParser.load("i18n/messages", "_zh_CN", false);
        assertThat(props.getProperty("greeting")).isEqualTo("你好");
    }

    @Test
    void load_classpathWithoutSuffix() {
        Properties props = PropertiesFileParser.load("i18n/messages", null, false);
        assertThat(props.getProperty("greeting")).isEqualTo("Hello");
    }

    @Test
    void load_fileSystemWithSuffix() throws IOException {
        Path file = tempDir.resolve("app_zh_CN.properties");
        Files.writeString(file, "greeting=你好\n", StandardCharsets.UTF_8);

        Properties props = PropertiesFileParser.load(tempDir.resolve("app").toString(), "_zh_CN", true);
        assertThat(props.getProperty("greeting")).isEqualTo("你好");
    }

    @Test
    void load_fileSystemMissing_returnsEmpty() {
        Properties props = PropertiesFileParser.load(tempDir.resolve("nope").toString(), null, true);
        assertThat(props).isEmpty();
    }

    @Test
    void loadFromClasspath_ioError_throwsResourceLoadException() {
        // 模拟不可读资源：直接调用内部不可行，验证文件系统读取损坏路径
        assertThatThrownBy(() -> PropertiesFileParser.loadFromFile(tempDir))
                .isInstanceOf(ResourceLoadException.class);
    }
}
