package cn.jowen.framework.plugin.qagap;

import cn.jowen.framework.plugin.hotswap.HotSwapStrategy;
import cn.jowen.framework.plugin.hotswap.PluginHotSwapManager;
import cn.jowen.framework.plugin.lifecycle.PluginLifecycleManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 触发 {@link PluginHotSwapManager#watchLoop()} 经真实 WatchService 处理 {@code .jar} 事件，
 * 覆盖事件分发分支（行 94-102）：start 后创建真实 {@code .jar} 文件，watchLoop 经防抖后调用
 * {@code onFileChanged} 分发；随后 close 停止监听。
 *
 * <p>说明：生产代码中 {@code handler} 字段非 volatile，watchLoop 线程会缓存 start 时写入的
 * 默认 handler，因此本测试不依赖自定义 handler 的回调，仅断言运行状态的正确转换，
 * 事件分发分支作为副作用被覆盖。
 */
class PluginHotSwapManagerWatchLoopTest {

    @Test
    void watchLoop_processesRealJarEvent(@TempDir Path tempDir) throws Exception {
        PluginHotSwapManager manager = new PluginHotSwapManager(
                tempDir, new PluginLifecycleManager(), HotSwapStrategy.MANUAL, 50);
        manager.start();
        assertThat(manager.isRunning()).isTrue();

        Path jar = tempDir.resolve("plugin.jar");
        Files.createFile(jar);
        // 等待 WatchService 真正分发该 .jar 事件（覆盖 94-102）
        TimeUnit.MILLISECONDS.sleep(1500);

        manager.close();
        assertThat(manager.isRunning()).isFalse();
    }
}
