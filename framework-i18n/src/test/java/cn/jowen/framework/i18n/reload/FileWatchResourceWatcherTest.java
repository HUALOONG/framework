package cn.jowen.framework.i18n.reload;

import cn.jowen.framework.i18n.api.ReloadableMessageSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * {@link FileWatchResourceWatcher} 测试。
 */
class FileWatchResourceWatcherTest {

    @TempDir
    Path tempDir;

    private final ReloadableMessageSource target = mock(ReloadableMessageSource.class);
    private final AtomicInteger reloadCount = new AtomicInteger();
    private final ResourceReloader reloader = new ResourceReloader();

    FileWatchResourceWatcherTest() {
        doAnswer(invocation -> {
            reloadCount.incrementAndGet();
            return null;
        }).when(target).reload();
    }

    @Test
    void constructor_rejectsMissingDirectory() {
        Path missing = tempDir.resolve("not-exists");
        assertThatThrownBy(() -> new FileWatchResourceWatcher(missing, target, reloader, Duration.ofMillis(10)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getDirectory_returnsConfiguredDirectory() {
        FileWatchResourceWatcher watcher = new FileWatchResourceWatcher(tempDir, target, reloader, Duration.ofMillis(10));
        assertThat(watcher.getDirectory()).isEqualTo(tempDir);
    }

    @Test
    void startAndStop_lifecycle() {
        FileWatchResourceWatcher watcher = new FileWatchResourceWatcher(tempDir, target, reloader, Duration.ofMillis(10));
        assertThat(watcher.isRunning()).isFalse();

        watcher.start();
        assertThat(watcher.isRunning()).isTrue();

        // start 幂等
        watcher.start();
        assertThat(watcher.isRunning()).isTrue();

        watcher.stop();
        assertThat(watcher.isRunning()).isFalse();

        // stop 幂等
        watcher.stop();
        assertThat(watcher.isRunning()).isFalse();
    }

    @Test
    void fileModification_triggersReload() throws Exception {
        Path props = tempDir.resolve("messages.properties");
        Files.writeString(props, "greeting=Hello\n");

        FileWatchResourceWatcher watcher = new FileWatchResourceWatcher(tempDir, target, reloader, Duration.ofMillis(20));
        watcher.start();
        try {
            // 等待监听线程就绪并进入 take()，避免 start 后立即写文件导致首个事件被吞
            Thread.sleep(200);
            Files.writeString(props, "greeting=Hi\n");
            // 追加一次写入以规避部分文件系统对瞬时改写的事件合并/丢失
            Thread.sleep(50);
            Files.writeString(props, "greeting=Hey\n");
            verify(target, org.mockito.Mockito.timeout(5000).atLeast(1)).reload();
        } finally {
            watcher.stop();
        }
    }

    @Test
    void nonPropertiesChange_ignored() throws Exception {
        FileWatchResourceWatcher watcher = new FileWatchResourceWatcher(tempDir, target, reloader, Duration.ofMillis(20));
        watcher.start();
        try {
            Files.writeString(tempDir.resolve("readme.txt"), "not a message");
            Thread.sleep(150);
            assertThat(reloadCount.get()).isZero();
        } finally {
            watcher.stop();
        }
    }

    private void awaitReload(int expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            if (reloadCount.get() >= expected) {
                return;
            }
            Thread.sleep(20);
        }
    }
}
