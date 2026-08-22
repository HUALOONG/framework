package cn.jowen.framework.i18n.reload;

import cn.jowen.framework.i18n.source.PropertiesMessageSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class FileWatchResourceWatcherTest {

    @TempDir
    Path tempDir;

    private static final Duration DEBOUNCE = Duration.ofMillis(300);

    private PropertiesMessageSource createSource() throws IOException {
        Path file = tempDir.resolve("messages.properties");
        Files.writeString(file, "greeting=Hello", StandardCharsets.UTF_8);
        // file: 前缀后跟 basename（不含 .properties 后缀）
        return new PropertiesMessageSource("file:" + tempDir.resolve("messages"));
    }

    @Test
    void reloadsOnFileModification() throws Exception {
        PropertiesMessageSource source = createSource();
        assertThat(source.getMessage("greeting", Locale.ENGLISH, null)).isEqualTo("Hello");

        ResourceReloader reloader = new ResourceReloader();
        FileWatchResourceWatcher watcher = new FileWatchResourceWatcher(tempDir, source, reloader, DEBOUNCE);
        watcher.start();
        try {
            Path file = tempDir.resolve("messages.properties");
            Files.writeString(file, "greeting=Hello World", StandardCharsets.UTF_8);

            await(() -> "Hello World".equals(source.getMessage("greeting", Locale.ENGLISH, null)));
            assertThat(watcher.isRunning()).isTrue();
        } finally {
            watcher.stop();
        }
        assertThat(watcher.isRunning()).isFalse();
    }

    @Test
    void ignoresNonPropertiesChanges() throws Exception {
        PropertiesMessageSource source = createSource();
        ResourceReloader reloader = new ResourceReloader();
        FileWatchResourceWatcher watcher = new FileWatchResourceWatcher(tempDir, source, reloader, DEBOUNCE);
        watcher.start();
        try {
            Path other = tempDir.resolve("readme.txt");
            Files.writeString(other, "not a bundle", StandardCharsets.UTF_8);
            Thread.sleep(DEBOUNCE.toMillis() + 200);
            assertThat(source.getMessage("greeting", Locale.ENGLISH, null)).isEqualTo("Hello");
        } finally {
            watcher.stop();
        }
    }

    @Test
    void startIsIdempotentAndStopsCleanly() throws Exception {
        PropertiesMessageSource source = createSource();
        ResourceReloader reloader = new ResourceReloader();
        FileWatchResourceWatcher watcher = new FileWatchResourceWatcher(tempDir, source, reloader, DEBOUNCE);
        watcher.start();
        watcher.start();
        assertThat(watcher.isRunning()).isTrue();
        watcher.stop();
        watcher.stop();
        assertThat(watcher.isRunning()).isFalse();
    }

    private static void await(CheckedBoolean condition) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.evaluate()) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("等待条件超时");
    }

    @FunctionalInterface
    interface CheckedBoolean {
        boolean evaluate();
    }
}
