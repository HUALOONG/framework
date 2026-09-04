package cn.jowen.framework.i18n.source;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertiesMessageSourceTest {

    @TempDir
    Path tempDir;

    private String fileBase() {
        return "file:" + tempDir.resolve("messages").toString();
    }

    @Test
    void getMessage_resolvesLanguageAndCountryAndFallback() throws Exception {
        Files.write(tempDir.resolve("messages.properties"), "greeting=hello\nonlybase=base\n".getBytes(StandardCharsets.UTF_8));
        Files.write(tempDir.resolve("messages_zh.properties"), "greeting=ni hao\n".getBytes(StandardCharsets.UTF_8));
        Files.write(tempDir.resolve("messages_zh_CN.properties"), "greeting=ni hao cn\n".getBytes(StandardCharsets.UTF_8));

        PropertiesMessageSource source = new PropertiesMessageSource(fileBase());

        assertThat(source.getMessage("greeting", Locale.ROOT, null)).isEqualTo("hello");
        assertThat(source.getMessage("greeting", Locale.CHINESE, null)).isEqualTo("ni hao");
        assertThat(source.getMessage("greeting", Locale.CHINA, null)).isEqualTo("ni hao cn");
        // 回退：zh_CN 无 onlybase，回退到默认
        assertThat(source.getMessage("onlybase", Locale.CHINA, null)).isEqualTo("base");
        // 不存在的 code 返回 null
        assertThat(source.getMessage("missing", Locale.ROOT, null)).isNull();
    }

    @Test
    void getMessage_withArgs_formats() throws Exception {
        Files.write(tempDir.resolve("messages.properties"), "tpl=hello {0}, you are {1}\n".getBytes(StandardCharsets.UTF_8));
        PropertiesMessageSource source = new PropertiesMessageSource(fileBase());
        assertThat(source.getMessage("tpl", Locale.ROOT, new Object[]{"world", 42})).isEqualTo("hello world, you are 42");
    }

    @Test
    void contains_and_messageCount() throws Exception {
        Files.write(tempDir.resolve("messages.properties"), "a=1\nb=2\n".getBytes(StandardCharsets.UTF_8));
        Files.write(tempDir.resolve("messages_zh.properties"), "a=一\n".getBytes(StandardCharsets.UTF_8));
        PropertiesMessageSource source = new PropertiesMessageSource(fileBase());

        assertThat(source.contains("a", Locale.ROOT)).isTrue();
        assertThat(source.contains("z", Locale.ROOT)).isFalse();
        // 触发两种 locale 的 bundle 加载后计数。
        // 注意：ROOT 以 "_" 为键加载 messages.properties(2 条)，CHINESE 以 "zh_" 为键加载
        // messages_zh.properties(1 条)，并在默认回退时以 "" 为键再次加载 messages.properties(2 条)，
        // 二者键不同故不合并，总计 5 条。
        source.getMessage("a", Locale.ROOT, null);
        source.getMessage("a", Locale.CHINESE, null);
        assertThat(source.messageCount()).isEqualTo(5);
    }

    @Test
    void reload_reloadsExistingLocales() throws Exception {
        Files.write(tempDir.resolve("messages_zh_CN.properties"), "k=v1\n".getBytes(StandardCharsets.UTF_8));
        PropertiesMessageSource source = new PropertiesMessageSource(fileBase());
        assertThat(source.getMessage("k", Locale.CHINA, null)).isEqualTo("v1");

        Files.write(tempDir.resolve("messages_zh_CN.properties"), "k=v2\n".getBytes(StandardCharsets.UTF_8));
        source.reload();
        assertThat(source.getMessage("k", Locale.CHINA, null)).isEqualTo("v2");
    }

    @Test
    void nonExistentFileBase_returnsNull() {
        PropertiesMessageSource source = new PropertiesMessageSource("file:" + tempDir.resolve("nope").toString());
        assertThat(source.getMessage("any", Locale.ROOT, null)).isNull();
        assertThat(source.contains("any", Locale.ROOT)).isFalse();
    }

    @Test
    void classpathBase_missingResource_returnsNull() {
        PropertiesMessageSource source = new PropertiesMessageSource("i18n/does-not-exist-messages");
        assertThat(source.getMessage("any", Locale.ROOT, null)).isNull();
    }

    @Test
    void getMessage_withEmptyArgs_returnsRawTemplate() throws Exception {
        // 覆盖 getMessage 中 `args == null || args.length == 0` 的 args.length == 0 分支：
        // 空数组（非 null）同样视为“无占位符替换”，直接返回原始模板。
        Files.write(tempDir.resolve("messages.properties"), "tpl=hello {0}\n".getBytes(StandardCharsets.UTF_8));
        PropertiesMessageSource source = new PropertiesMessageSource(fileBase());
        assertThat(source.getMessage("tpl", Locale.ROOT, new Object[0])).isEqualTo("hello {0}");
    }

    @Test
    void getMessage_languageLevelFallbackReturnsValue() throws Exception {
        // 覆盖 resolveRaw 的语言级回退（line 119-120）：zh_CN 缺失的 code 回退到 zh_ 命中。
        Files.write(tempDir.resolve("messages.properties"), "base=base\n".getBytes(StandardCharsets.UTF_8));
        Files.write(tempDir.resolve("messages_zh.properties"), "greeting=ni hao\nonlyzh=zh only\n".getBytes(StandardCharsets.UTF_8));
        Files.write(tempDir.resolve("messages_zh_CN.properties"), "greeting=ni hao cn\n".getBytes(StandardCharsets.UTF_8));

        PropertiesMessageSource source = new PropertiesMessageSource(fileBase());
        assertThat(source.getMessage("onlyzh", Locale.CHINA, null)).isEqualTo("zh only");
        // 同时确认默认回退仍工作
        assertThat(source.getMessage("base", Locale.CHINA, null)).isEqualTo("base");
    }

    @Test
    void reload_keepsOldValueWhenFileTemporarilyUnreadable() throws Exception {
        // 覆盖 reload() 中 `if (fresh.isEmpty() && !entry.getValue().isEmpty())` 分支（line 98-100）：
        // 重载时资源临时不可读，保留旧快照，避免文案丢失。
        Files.write(tempDir.resolve("messages_zh_CN.properties"), "k=v1\n".getBytes(StandardCharsets.UTF_8));
        PropertiesMessageSource source = new PropertiesMessageSource(fileBase());
        assertThat(source.getMessage("k", Locale.CHINA, null)).isEqualTo("v1");

        // 模拟资源被短暂删除：重载期间读不到，但旧值非空，应保留旧快照
        Files.delete(tempDir.resolve("messages_zh_CN.properties"));
        source.reload();
        assertThat(source.getMessage("k", Locale.CHINA, null)).isEqualTo("v1");
    }

    @Test
    void loadFromFile_ioExceptionPropagatesAsUnchecked() throws Exception {
        // 覆盖 loadFromFile 的 IOException 分支（line 147-148）：
        // 在预期的文件路径上放一个“同名目录”，使 newBufferedReader 抛 IOException。
        Path dir = tempDir.resolve("messages.properties");
        Files.createDirectory(dir);

        PropertiesMessageSource source = new PropertiesMessageSource("file:" + tempDir.resolve("messages").toString());
        assertThatThrownBy(() -> source.getMessage("any", Locale.ROOT, null))
                .isInstanceOf(UncheckedIOException.class);
    }

    @Test
    void loadFromClasspath_ioExceptionIsIgnored() {
        // 覆盖 loadFromClasspath 的 IOException catch 分支（line 157）：
        // 通过自定义上下文类加载器，使 getResourceAsStream 返回一个读取即抛 IOException 的流，
        // 验证该异常被吞掉（正常回退场景），getMessage 返回 null。
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        ClassLoader throwing = new ClassLoader() {
            @Override
            public InputStream getResourceAsStream(String name) {
                return new InputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new IOException("simulated classpath read failure");
                    }
                };
            }
        };
        Thread.currentThread().setContextClassLoader(throwing);
        try {
            PropertiesMessageSource source = new PropertiesMessageSource("i18n/simulated");
            assertThat(source.getMessage("any", Locale.ROOT, null)).isNull();
            assertThat(source.contains("any", Locale.ROOT)).isFalse();
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }
}
