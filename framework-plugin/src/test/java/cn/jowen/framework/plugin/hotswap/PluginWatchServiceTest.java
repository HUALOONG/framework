package cn.jowen.framework.plugin.hotswap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatCode;

class PluginWatchServiceTest {

    @TempDir
    Path tempDir;

    private HotSwapStrategy noopStrategy() {
        return fileName -> {};
    }

    @Test
    void closeDoesNotThrow() {
        PluginWatchService service = new PluginWatchService(tempDir, noopStrategy(), 100L);
        service.start();
        assertThatCode(service::close).doesNotThrowAnyException();
    }

    @Test
    void multipleCloseCallsSafe() {
        PluginWatchService service = new PluginWatchService(tempDir, noopStrategy(), 100L);
        service.start();
        service.close();
        assertThatCode(service::close).doesNotThrowAnyException();
    }
}
