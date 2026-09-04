package cn.jowen.framework.extras.storage.local;

import cn.jowen.framework.extras.storage.FileStorage;
import cn.jowen.framework.extras.storage.exception.StorageException;
import cn.jowen.framework.extras.properties.StorageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    private LocalFileStorage storage() {
        return new LocalFileStorage(tempDir.toString());
    }

    @Test
    void type_isLocal() {
        assertThat(storage().type()).isEqualTo(StorageType.LOCAL);
    }

    @Test
    void put_and_get_and_exists() throws IOException {
        LocalFileStorage storage = storage();
        String key = "docs/note.txt";
        storage.put(key, new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)), null);

        assertThat(storage.exists(key)).isTrue();
        try (var in = storage.get(key)) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("hello");
        }
    }

    @Test
    void get_missing_returnsNull() {
        assertThat(storage().get("absent.bin")).isNull();
        assertThat(storage().exists("absent.bin")).isFalse();
    }

    @Test
    void delete_removesFile() {
        LocalFileStorage storage = storage();
        storage.put("a.txt", new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)), null);
        assertThat(storage.exists("a.txt")).isTrue();
        storage.delete("a.txt");
        assertThat(storage.exists("a.txt")).isFalse();
    }

    @Test
    void generateUrl_returnsFileUrl() {
        LocalFileStorage storage = storage();
        storage.put("u.txt", new ByteArrayInputStream("y".getBytes(StandardCharsets.UTF_8)), null);
        assertThat(storage.generateUrl("u.txt", 1000).toString()).contains("u.txt");
    }

    @Test
    void put_pathTraversal_rejected() {
        LocalFileStorage storage = storage();
        assertThatThrownBy(() -> storage.put("../evil.txt",
                new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)), null))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("非法存储路径");
    }

    @Test
    void get_pathTraversal_rejected() {
        LocalFileStorage storage = storage();
        assertThatThrownBy(() -> storage.get("../evil.txt"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("非法存储路径");
    }

    @Test
    void delete_pathTraversal_rejected() {
        LocalFileStorage storage = storage();
        assertThatThrownBy(() -> storage.delete("../evil.txt"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("非法存储路径");
    }

    @Test
    void get_directory_throwsReadFailure() throws IOException {
        LocalFileStorage storage = storage();
        Files.createDirectory(tempDir.resolve("dir"));
        assertThatThrownBy(() -> storage.get("dir"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("本地文件读取失败");
    }

    @Test
    void delete_reservedDeviceName_throwsDeleteFailure() {
        // On Windows a reserved device name such as NUL resolves to the device and
        // Files.deleteIfExists raises a FileSystemException (IOException), exercising the
        // delete failure-translation path. This is a Windows-only deterministic trigger.
        LocalFileStorage storage = storage();
        assertThatThrownBy(() -> storage.delete("NUL"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("本地文件删除失败");
    }

    @Test
    void put_parentIsAFile_throwsWriteFailure() throws IOException {
        LocalFileStorage storage = storage();
        Files.write(tempDir.resolve("blocker"), "x".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> storage.put("blocker/child.txt",
                new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)), null))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("本地文件写入失败");
    }

    @Test
    void generateUrl_pathTraversal_declaresFailure() {
        LocalFileStorage storage = storage();
        assertThatThrownBy(() -> storage.generateUrl("../evil.txt", 0))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("生成访问 URL 失败");
    }
}
