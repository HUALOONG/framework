package cn.jowen.framework.extras.storage;

import cn.jowen.framework.extras.properties.StorageType;
import cn.jowen.framework.extras.storage.exception.StorageException;
import cn.jowen.framework.extras.storage.local.LocalFileStorage;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link LocalFileStorage} 本地存储验证。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    private LocalFileStorage storage() {
        return new LocalFileStorage(tempDir.toString());
    }

    private static InputStream streamOf(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void typeIsLocal() {
        assertThat(storage().type()).isEqualTo(StorageType.LOCAL);
    }

    @Test
    void putAndGetRoundTrip() throws Exception {
        LocalFileStorage storage = storage();
        storage.put("a/b.txt", streamOf("content"), "text/plain");

        assertThat(storage.exists("a/b.txt")).isTrue();
        try (InputStream in = storage.get("a/b.txt")) {
            assertThat(in).isNotNull();
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("content");
        }
    }

    @Test
    void getReturnsNullWhenMissing() {
        assertThat(storage().get("missing.txt")).isNull();
    }

    @Test
    void deleteRemovesFile() {
        LocalFileStorage storage = storage();
        storage.put("del.txt", streamOf("x"), null);
        storage.delete("del.txt");
        assertThat(storage.exists("del.txt")).isFalse();
    }

    @Test
    void deleteIsIdempotent() {
        LocalFileStorage storage = storage();
        storage.delete("never-existed.txt");
        assertThat(storage.exists("never-existed.txt")).isFalse();
    }

    @Test
    void pathTraversalIsRejected() {
        LocalFileStorage storage = storage();
        assertThatThrownBy(() -> storage.put("../escape.txt", streamOf("x"), null))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("非法存储路径");
    }

    @Test
    void generateUrlPointsToFile() {
        LocalFileStorage storage = storage();
        storage.put("u.txt", streamOf("x"), null);
        assertThat(storage.generateUrl("u.txt", 0).toString()).endsWith("u.txt");
    }
}
