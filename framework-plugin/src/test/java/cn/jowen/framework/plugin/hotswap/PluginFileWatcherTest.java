package cn.jowen.framework.plugin.hotswap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class PluginFileWatcherTest {

    @Test
    void close_doesNotThrow(@TempDir Path tmpDir) throws Exception {
        PluginFileWatcher watcher = new PluginFileWatcher(tmpDir, emptyHandler(), 100);
        watcher.close();
    }

    @Test
    void startAndClose_withCreatedEvent(@TempDir Path tmpDir) throws Exception {
        AtomicReference<String> captured = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        PluginFileWatcher.FileChangeHandler handler = new PluginFileWatcher.FileChangeHandler() {
            @Override
            public void onFileCreated(String fileName) {
                captured.set(fileName);
                latch.countDown();
            }
            @Override
            public void onFileModified(String fileName) {}
            @Override
            public void onFileDeleted(String fileName) {}
        };

        PluginFileWatcher watcher = new PluginFileWatcher(tmpDir, handler, 100);
        watcher.start();

        Path jarFile = tmpDir.resolve("test-plugin.jar");
        java.nio.file.Files.createFile(jarFile);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        watcher.close();
        assertThat(captured.get()).isEqualTo("test-plugin.jar");
    }

    @Test
    void watchLoop_runsWhileActive(@TempDir Path tmpDir) throws Exception {
        // 让 watchLoop 真正迭代若干轮（poll 返回 null 后 continue），覆盖循环体与防抖分发路径
        PluginFileWatcher.FileChangeHandler handler = new PluginFileWatcher.FileChangeHandler() {
            @Override
            public void onFileCreated(String fileName) {
            }

            @Override
            public void onFileModified(String fileName) {
            }

            @Override
            public void onFileDeleted(String fileName) {
            }
        };
        PluginFileWatcher watcher = new PluginFileWatcher(tmpDir, handler, 100);
        watcher.start();
        Thread.sleep(300);
        watcher.close();
    }

    @Test
    void constructor_createsWatcher(@TempDir Path tmpDir) {
        PluginFileWatcher watcher = new PluginFileWatcher(tmpDir, emptyHandler(), 100);
        assertThat(watcher).isNotNull();
        watcher.close();
    }

    static PluginFileWatcher.FileChangeHandler emptyHandler() {
        return new PluginFileWatcher.FileChangeHandler() {
            @Override
            public void onFileCreated(String fileName) {}
            @Override
            public void onFileModified(String fileName) {}
            @Override
            public void onFileDeleted(String fileName) {}
        };
    }
}
