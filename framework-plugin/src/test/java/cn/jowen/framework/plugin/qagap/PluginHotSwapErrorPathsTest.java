package cn.jowen.framework.plugin.qagap;

import cn.jowen.framework.core.util.ReflectionUtils;
import cn.jowen.framework.plugin.hotswap.HotSwapStrategy;
import cn.jowen.framework.plugin.hotswap.PluginFileWatcher;
import cn.jowen.framework.plugin.hotswap.PluginHotSwapManager;
import cn.jowen.framework.plugin.lifecycle.PluginLifecycleManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * {@link PluginHotSwapManager} 错误路径补充覆盖：
 * <ul>
 *   <li>start 时插件目录非法（普通文件而非目录）导致内部 WatchServiceWrapper 构造抛出
 *       NotDirectoryException，被 watchLoop 的 catch 捕获并记录（覆盖 line 104-105）；</li>
 *   <li>close 时 handler 为“关闭时抛 IOException”的 Closeable，被 close 的 instanceof 分支
 *       catch 捕获并记录（覆盖 line 146-147）。</li>
 * </ul>
 * 这两个分支均为既有测试未触达的生产容错逻辑，异常不应向上传播。
 */
class PluginHotSwapErrorPathsTest {

    /** 关闭时抛出 IOException 的 Closeable handler，用于覆盖 close 中的 catch 分支。 */
    static class ThrowingCloseableHandler implements PluginFileWatcher.FileChangeHandler, Closeable {
        @Override
        public void onFileCreated(String fileName) {
        }

        @Override
        public void onFileModified(String fileName) {
        }

        @Override
        public void onFileDeleted(String fileName) {
        }

        @Override
        public void close() throws IOException {
            throw new IOException("模拟关闭失败");
        }
    }

    @Test
    void start_withNonDirectoryPath_catchesWatchLoopError(@TempDir Path tempDir) {
        Path notADir = tempDir.resolve("not-a-directory.txt");
        // 创建一个普通文件（非目录），作为非法 pluginsDir
        assertThatCode(() -> Files.createFile(notADir)).doesNotThrowAnyException();

        PluginHotSwapManager manager = new PluginHotSwapManager(
                notADir, new PluginLifecycleManager(), HotSwapStrategy.MANUAL, 50);
        assertThat(manager.isRunning()).isFalse();

        // start 启动 watchLoop 线程；非法目录使 WatchServiceWrapper 构造抛 NotDirectoryException，
        // 被 watchLoop 的 catch 捕获并记录（line 104-105），异常不传播到调用方
        manager.start();
        assertThat(manager.isRunning()).isTrue();

        // close 幂等收尾
        assertThatCode(manager::close).doesNotThrowAnyException();
        assertThat(manager.isRunning()).isFalse();
    }

    @Test
    void close_withThrowingCloseableHandler_logsWarning(@TempDir Path tempDir) {
        PluginHotSwapManager manager = new PluginHotSwapManager(
                tempDir, new PluginLifecycleManager(), HotSwapStrategy.MANUAL, 50);
        // 注入一个关闭时抛 IOException 的 Closeable handler（未 start，避免真实监听线程干扰）
        ReflectionUtils.setFieldValue(manager, "handler", new ThrowingCloseableHandler());

        // close 时 instanceof Closeable 分支命中，closeable.close() 抛 IOException 被 catch 记录
        // （line 146-147），异常不应传播到调用方
        assertThatCode(manager::close).doesNotThrowAnyException();
    }
}
