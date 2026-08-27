package cn.jowen.framework.plugin.support;

import cn.jowen.framework.plugin.descriptor.PluginDescriptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PluginPackageTest {

    @Test
    void pack_createsJarFile(@TempDir Path tmpDir) throws IOException {
        PluginDescriptor desc = PluginDescriptor.of("test-plugin", "1.0.0", "com.example.TestPlugin");
        Path classesDir = tmpDir.resolve("classes");
        Files.createDirectories(classesDir);
        Path output = tmpDir.resolve("test-plugin.jar");
        PluginPackage.pack(desc, classesDir, List.of(), output);
        assertThat(output).exists();
    }

    @Test
    void pack_withClasses(@TempDir Path tmpDir) throws IOException {
        PluginDescriptor desc = PluginDescriptor.of("test-plugin", "1.0.0", "com.example.TestPlugin");
        Path classesDir = tmpDir.resolve("classes");
        Files.createDirectories(classesDir);
        Path output = tmpDir.resolve("test-plugin.jar");
        PluginPackage.pack(desc, classesDir, List.of(), output);
        assertThat(output).exists();
    }
}
