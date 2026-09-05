package cn.jowen.framework.plugin.qagap;

import cn.jowen.framework.core.util.ReflectionUtils;
import cn.jowen.framework.plugin.hotswap.PluginFileWatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * {@link PluginFileWatcher} 补充覆盖：
 * <ul>
 *   <li>真实 MODIFY 事件触发 onFileModified 回调（line 88）；</li>
 *   <li>监听线程在 poll 期间被中断，走 InterruptedException 的 catch 分支（line 73-75）。</li>
 * </ul>
 * 注：onFileDeleted（line 89）因 watchLoop 中 isRegularFile 守卫对“已删除文件”提前 continue 而不可达，
 * 属既有设计限制，不纳入覆盖目标；CREATE 事件已由既有 PluginFileWatcherTest 覆盖。
 */
class PluginFileWatcherErrorPathsTest {

    private static PluginFileWatcher.FileChangeHandler noOpHandler() {
        return new PluginFileWatcher.FileChangeHandler() {
            @Override
            public void onFileCreated(String n) {
            }

            @Override
            public void onFileModified(String n) {
            }

            @Override
            public void onFileDeleted(String n) {
            }
        };
    }

    @Test
    void watchLoop_reportsModifiedFileEvent(@TempDir Path tmpDir) throws Exception {
        AtomicReference<String> captured = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        PluginFileWatcher.FileChangeHandler handler = new PluginFileWatcher.FileChangeHandler() {
            @Override
            public void onFileCreated(String n) {
                // 先创建文件再启动监听，确保只产生 MODIFY 事件（避免与 CREATE 事件防抖合并）
            }

            @Override
            public void onFileModified(String n) {
                captured.set(n);
                latch.countDown();
            }

            @Override
            public void onFileDeleted(String n) {
            }
        };

        // 启动监听前先创建文件，确保后续只产生 MODIFY 事件
        Path jar = tmpDir.resolve("plugin.jar");
        Files.createFile(jar);

        PluginFileWatcher watcher = new PluginFileWatcher(tmpDir, handler, 100);
        watcher.start();

        // 写入触发 ENTRY_MODIFY（文件仍存在，满足 isRegularFile 守卫）
        Files.writeString(jar, "modified-content");

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        watcher.close();

        assertThat(completed).isTrue();
        assertThat(captured.get()).isEqualTo("plugin.jar");
    }

    @Test
    void watchLoop_interruptDuringPoll_exitsViaCatch(@TempDir Path tmpDir) throws Exception {
        PluginFileWatcher watcher = new PluginFileWatcher(tmpDir, noOpHandler(), 100);
        watcher.start();

        // 获取私有监听线程并中断：poll(500ms) 会抛出 InterruptedException，
        // 被 watchLoop 的 catch 捕获（line 73-75），线程正常退出
        Thread watcherThread = (Thread) ReflectionUtils.getFieldValue(watcher, "watcherThread");
        assertThat(watcherThread).isNotNull();
        watcherThread.interrupt();
        watcherThread.join(2000);

        assertThatCode(watcher::close).doesNotThrowAnyException();
    }
}
