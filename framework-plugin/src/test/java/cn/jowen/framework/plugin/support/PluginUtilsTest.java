package cn.jowen.framework.plugin.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class PluginUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void isValidPluginId_acceptsValid() {
        assertThat(PluginUtils.isValidPluginId("com.example.plugin")).isTrue();
        assertThat(PluginUtils.isValidPluginId("a.b:c-d_1")).isTrue();
    }

    @Test
    void isValidPluginId_rejectsInvalid() {
        assertThat(PluginUtils.isValidPluginId(null)).isFalse();
        assertThat(PluginUtils.isValidPluginId("")).isFalse();
        assertThat(PluginUtils.isValidPluginId("a")).isFalse();
        assertThat(PluginUtils.isValidPluginId("has space")).isFalse();
        assertThat(PluginUtils.isValidPluginId("toolongid".repeat(10))).isFalse();
    }

    @Test
    void extractPluginJars_findsTopLevelJars() throws Exception {
        Path plugins = tempDir.resolve("plugins");
        Files.createDirectories(plugins);
        Path main1 = plugins.resolve("a.jar");
        Path main2 = plugins.resolve("b.jar");
        Path notJar = plugins.resolve("readme.txt");
        Files.createFile(main1);
        Files.createFile(main2);
        Files.createFile(notJar);

        List<Path> jars = PluginUtils.extractPluginJars(plugins);
        assertThat(jars).containsExactlyInAnyOrder(main1, main2);
    }

    @Test
    void extractPluginJars_nonDirectory_returnsEmpty() throws Exception {
        Path notDir = tempDir.resolve("file.txt");
        Files.createFile(notDir);
        assertThat(PluginUtils.extractPluginJars(notDir)).isEmpty();
    }

    @Test
    void calculateChecksum_isStable() throws Exception {
        Path f = tempDir.resolve("x.bin");
        Files.write(f, new byte[]{1, 2, 3, 4});
        String h1 = PluginUtils.calculateChecksum(f);
        String h2 = PluginUtils.calculateChecksum(f);
        assertThat(h1).hasSize(64).isEqualTo(h2);
    }

    @Test
    void getPluginLibs_extractsLibEntries() throws Exception {
        Path pluginJar = tempDir.resolve("plugin.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(pluginJar))) {
            jos.putNextEntry(new JarEntry("lib/dep.jar"));
            jos.write(new byte[]{1, 2, 3});
            jos.closeEntry();
            // 非 lib/ 开头的条目应被忽略
            jos.putNextEntry(new JarEntry("META-INF/plugin/plugin.json"));
            jos.write("{}".getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }

        List<Path> libs = PluginUtils.getPluginLibs(pluginJar);

        assertThat(libs).hasSize(1);
        assertThat(libs.get(0).getFileName().toString()).isEqualTo("dep.jar");
        assertThat(libs.get(0)).exists();
    }

    @Test
    void getPluginLibs_alreadyExtracted_reusesExistingFile() throws Exception {
        Path pluginJar = tempDir.resolve("plugin2.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(pluginJar))) {
            jos.putNextEntry(new JarEntry("lib/dep.jar"));
            jos.write(new byte[]{9, 9});
            jos.closeEntry();
        }
        // 预先在同目录放置同名文件，覆盖 "已存在则跳过解压" 分支
        Path existing = tempDir.resolve("dep.jar");
        Files.write(existing, new byte[]{7, 7, 7});

        List<Path> libs = PluginUtils.getPluginLibs(pluginJar);

        assertThat(libs).containsExactly(existing);
        // 未被覆盖，仍为原有内容
        assertThat(Files.readAllBytes(existing)).hasSize(3);
    }

    @Test
    void getPluginLibs_withoutLibEntries_returnsEmpty() throws Exception {
        Path pluginJar = tempDir.resolve("plugin3.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(pluginJar))) {
            jos.putNextEntry(new JarEntry("META-INF/plugin/plugin.json"));
            jos.write("{}".getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }

        assertThat(PluginUtils.getPluginLibs(pluginJar)).isEmpty();
    }
}
