package cn.jowen.framework.plugin.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PluginUtilsTest {

    @Test
    void isValidPluginId_validId_returnsTrue() {
        assertThat(PluginUtils.isValidPluginId("my-plugin")).isTrue();
        assertThat(PluginUtils.isValidPluginId("com.example.plugin")).isTrue();
        assertThat(PluginUtils.isValidPluginId("plugin-01")).isTrue();
        assertThat(PluginUtils.isValidPluginId("a_b")).isTrue();
        assertThat(PluginUtils.isValidPluginId("plugin:sub")).isTrue();
    }

    @Test
    void isValidPluginId_tooShort_returnsFalse() {
        assertThat(PluginUtils.isValidPluginId("a")).isFalse();
    }

    @Test
    void isValidPluginId_empty_returnsFalse() {
        assertThat(PluginUtils.isValidPluginId("")).isFalse();
    }

    @Test
    void isValidPluginId_null_returnsFalse() {
        assertThat(PluginUtils.isValidPluginId(null)).isFalse();
    }

    @Test
    void isValidPluginId_invalidChars_returnsFalse() {
        assertThat(PluginUtils.isValidPluginId("invalid space")).isFalse();
        assertThat(PluginUtils.isValidPluginId("invalid@char")).isFalse();
        assertThat(PluginUtils.isValidPluginId("invalid#char")).isFalse();
    }

    @Test
    void extractPluginJars_emptyDir_returnsEmpty() throws Exception {
        java.nio.file.Path tmp = java.nio.file.Files.createTempDirectory("plugin-jars");
        assertThat(PluginUtils.extractPluginJars(tmp)).isEmpty();
    }

    @Test
    void extractPluginJars_excludesLibDir() throws Exception {
        java.nio.file.Path tmp = java.nio.file.Files.createTempDirectory("plugin-jars");
        java.nio.file.Files.createDirectories(tmp.resolve("lib"));
        java.nio.file.Files.createFile(tmp.resolve("main.jar"));
        java.nio.file.Files.createFile(tmp.resolve("lib/deps.jar"));
        var jars = PluginUtils.extractPluginJars(tmp);
        assertThat(jars).hasSize(1);
        assertThat(jars.getFirst().getFileName().toString()).isEqualTo("main.jar");
    }

    @Test
    void extractPluginJars_nonExistentDir_returnsEmpty() throws Exception {
        java.nio.file.Path nonExistent = java.nio.file.Path.of("/non/existent/dir");
        assertThat(PluginUtils.extractPluginJars(nonExistent)).isEmpty();
    }

    @Test
    void calculateChecksum_nonEmpty_file() throws Exception {
        java.nio.file.Path file = java.nio.file.Files.createTempFile("test", ".jar");
        String checksum = PluginUtils.calculateChecksum(file);
        assertThat(checksum).hasSize(64);
    }
}
