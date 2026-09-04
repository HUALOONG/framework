package cn.jowen.framework.plugin.support;

import cn.jowen.framework.plugin.descriptor.PluginDescriptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class PluginPackageTest {

    @TempDir
    Path tempDir;

    @Test
    void pack_writesPluginJsonAndClasses() throws IOException {
        PluginDescriptor d = PluginDescriptor.of("demo", "1.0.0", "com.demo.DemoPlugin");
        Path classesDir = tempDir.resolve("classes");
        Path classFile = classesDir.resolve("com/demo/Foo.class");
        Files.createDirectories(classFile.getParent());
        Files.write(classFile, new byte[]{1, 2, 3});

        Path out = tempDir.resolve("demo-plugin.jar");
        PluginPackage.pack(d, classesDir, List.of(), out);

        assertThat(Files.exists(out)).isTrue();
        try (JarFile jar = new JarFile(out.toFile())) {
            assertThat(jar.getEntry("plugin.json")).isNotNull();
            assertThat(jar.getEntry("com/demo/Foo.class")).isNotNull();
            try (InputStream is = jar.getInputStream(jar.getEntry("plugin.json"))) {
                String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                assertThat(json).contains("\"pluginId\": \"demo\"");
                assertThat(json).contains("\"pluginClass\": \"com.demo.DemoPlugin\"");
                assertThat(json).contains("\"enabledByDefault\": true");
            }
        }
    }

    @Test
    void pack_writesLibJars() throws IOException {
        PluginDescriptor d = PluginDescriptor.of("demo", "1.0.0", "com.demo.DemoPlugin");
        Path classesDir = tempDir.resolve("classes");
        Files.createDirectories(classesDir);

        Path libJar = tempDir.resolve("dep.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(libJar))) {
            jos.putNextEntry(new JarEntry("x.txt"));
            jos.write(0);
            jos.closeEntry();
        }

        Path out = tempDir.resolve("demo2-plugin.jar");
        PluginPackage.pack(d, classesDir, List.of(libJar), out);

        try (JarFile jar = new JarFile(out.toFile())) {
            assertThat(jar.getEntry("lib/dep.jar")).isNotNull();
        }
    }

    @Test
    void pack_nullLibJars_skipsLibSection() throws IOException {
        PluginDescriptor d = PluginDescriptor.of("demo", "1.0.0", "com.demo.DemoPlugin");
        Path classesDir = tempDir.resolve("classes");
        Files.createDirectories(classesDir);

        Path out = tempDir.resolve("demo3-plugin.jar");
        PluginPackage.pack(d, classesDir, null, out);

        assertThat(Files.exists(out)).isTrue();
    }
}
