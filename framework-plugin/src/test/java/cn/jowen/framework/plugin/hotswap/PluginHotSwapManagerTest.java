package cn.jowen.framework.plugin.hotswap;

import cn.jowen.framework.core.exception.SystemException;
import cn.jowen.framework.core.spi.ExtensionLoader;
import cn.jowen.framework.core.spi.SPI;
import cn.jowen.framework.core.util.ReflectionUtils;
import cn.jowen.framework.plugin.lifecycle.PluginLifecycleManager;
import cn.jowen.framework.plugin.registry.Extension;
import cn.jowen.framework.plugin.registry.ExtensionRegistry;
import cn.jowen.framework.plugin.spi.PluginSpiBridge;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link PluginHotSwapManager} 测试：关闭时清理桥接映射、start/close 生命周期、
 * WatchService 包装与 onFileChanged 分支。
 */
class PluginHotSwapManagerTest {

    @SPI
    public interface Point {
    }

    static final class PointImpl implements Point {
    }

    /** 记录调用的 FileChangeHandler（同时实现 Closeable，覆盖 close 中的 instanceof 分支）。 */
    static class RecordingHandler implements PluginFileWatcher.FileChangeHandler, Closeable {
        final List<String> modified = new ArrayList<>();
        final List<String> deleted = new ArrayList<>();
        boolean closed = false;

        @Override
        public void onFileCreated(String fileName) {
            modified.add(fileName);
        }

        @Override
        public void onFileModified(String fileName) {
            modified.add(fileName);
        }

        @Override
        public void onFileDeleted(String fileName) {
            deleted.add(fileName);
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    @Test
    void close_clearsBridgeMappings(@TempDir Path tempDir) {
        ExtensionRegistry registry = new ExtensionRegistry();
        PluginSpiBridge bridge = new PluginSpiBridge(registry);
        registry.register(new Extension("e1", Point.class.getName(), new PointImpl(), 0, "p1", null));
        bridge.registerExtensionPoint(Point.class.getName(), Point.class);
        ExtensionLoader<Point> loader = ExtensionLoader.getExtensionLoader(Point.class);
        assertThat(loader.getExtension("e1")).isInstanceOf(PointImpl.class);

        PluginHotSwapManager manager = new PluginHotSwapManager(
                tempDir, new PluginLifecycleManager(), HotSwapStrategy.RESTART, 100, bridge);

        manager.close();

        assertThatThrownBy(() -> loader.getExtension("e1"))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("未找到 SPI 实现");
    }

    @Test
    void start_runsWatcherAndCloseStopsIt(@TempDir Path tempDir) {
        PluginHotSwapManager manager = new PluginHotSwapManager(
                tempDir, new PluginLifecycleManager(), HotSwapStrategy.MANUAL, 50);
        assertThat(manager.isRunning()).isFalse();

        manager.start();
        assertThat(manager.isRunning()).isTrue();

        manager.close();
        assertThat(manager.isRunning()).isFalse();
    }

    @Test
    void close_withoutStart_isIdempotent(@TempDir Path tempDir) {
        PluginHotSwapManager manager = new PluginHotSwapManager(
                tempDir, new PluginLifecycleManager(), HotSwapStrategy.MANUAL, 50);
        assertThat(manager.isRunning()).isFalse();
        manager.close();
        assertThat(manager.isRunning()).isFalse();
    }

    @Test
    void onFileChanged_invokesHandlerForModifiedAndDeleted(@TempDir Path tempDir) throws IOException {
        PluginHotSwapManager manager = new PluginHotSwapManager(
                tempDir, new PluginLifecycleManager(), HotSwapStrategy.MANUAL, 50);
        RecordingHandler handler = new RecordingHandler();
        ReflectionUtils.setFieldValue(manager, "handler", handler);

        Path jar = tempDir.resolve("present.jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new JarEntry("META-INF/plugin/plugin.json"));
            out.write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            out.closeEntry();
        }

        // 直接驱动私有方法，避免依赖 WatchService 时序
        ReflectionUtils.invokeMethod(manager, "onFileChanged", "present.jar");
        ReflectionUtils.invokeMethod(manager, "onFileChanged", "absent.jar");

        assertThat(handler.modified).containsExactly("present.jar");
        assertThat(handler.deleted).containsExactly("absent.jar");

        // close 时 handler 为 Closeable，覆盖 instanceof 分支
        manager.close();
        assertThat(handler.closed).isTrue();
    }

    @Test
    void start_restartStrategySetsHandler(@TempDir Path tempDir) {
        PluginHotSwapManager manager = new PluginHotSwapManager(
                tempDir, new PluginLifecycleManager(), HotSwapStrategy.RESTART, 50);
        manager.start();
        assertThat(manager.isRunning()).isTrue();
        manager.close();
        assertThat(manager.isRunning()).isFalse();
    }

    @Test
    void start_twice_isIdempotent(@TempDir Path tempDir) {
        PluginHotSwapManager manager = new PluginHotSwapManager(
                tempDir, new PluginLifecycleManager(), HotSwapStrategy.MANUAL, 50);
        manager.start();
        // running 已为 true，二次 start 直接返回
        manager.start();
        assertThat(manager.isRunning()).isTrue();
        manager.close();
        assertThat(manager.isRunning()).isFalse();
    }

    @Test
    void start_reloadClassesStrategy_setsNullHandler(@TempDir Path tempDir) {
        PluginHotSwapManager manager = new PluginHotSwapManager(
                tempDir, new PluginLifecycleManager(), HotSwapStrategy.RELOAD_CLASSES, 50);
        manager.start();
        assertThat(manager.isRunning()).isTrue();
        // RELOAD_CLASSES 策略下 handler 为 null，onFileChanged 应直接返回且不抛异常
        ReflectionUtils.invokeMethod(manager, "onFileChanged", "x.jar");
        manager.close();
        assertThat(manager.isRunning()).isFalse();
    }

    @Test
    void watchLoop_runsWhileActive(@TempDir Path tempDir) throws Exception {
        // 让 watchLoop 真正构造 WatchServiceWrapper 并迭代若干轮，覆盖外层轮询与内部 WatchServiceWrapper 循环体
        PluginHotSwapManager manager = new PluginHotSwapManager(
                tempDir, new PluginLifecycleManager(), HotSwapStrategy.MANUAL, 50);
        manager.start();
        Thread.sleep(300);
        assertThat(manager.isRunning()).isTrue();
        manager.close();
        assertThat(manager.isRunning()).isFalse();
    }
}
