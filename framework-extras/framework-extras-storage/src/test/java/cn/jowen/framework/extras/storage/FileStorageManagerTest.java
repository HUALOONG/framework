package cn.jowen.framework.extras.storage;

import cn.jowen.framework.extras.storage.exception.StorageException;
import cn.jowen.framework.extras.storage.local.LocalFileStorage;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link FileStorageManager} 多桶路由验证。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
class FileStorageManagerTest {

    @TempDir
    Path tempDir;

    private FileStorageManager manager() {
        Map<String, FileStorage> map = new HashMap<>();
        map.put("local", new LocalFileStorage(tempDir.toString()));
        return new FileStorageManager(map, "local");
    }

    @Test
    void defaultBucketUsedWhenBucketBlank() {
        assertThat(manager().getStorage(null)).isInstanceOf(LocalFileStorage.class);
        assertThat(manager().getStorage("")).isInstanceOf(LocalFileStorage.class);
        assertThat(manager().getDefault()).isInstanceOf(LocalFileStorage.class);
    }

    @Test
    void unknownBucketThrows() {
        assertThatThrownBy(() -> manager().getStorage("nope"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("未注册的存储桶");
    }

    @Test
    void uploadReturnsFileInfo() {
        FileInfo info = manager().upload(null, "dir/photo.jpg",
                new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)), "image/jpeg");

        assertThat(info.bucket()).isEqualTo("local");
        assertThat(info.name()).isEqualTo("photo.jpg");
        assertThat(info.key()).isEqualTo("dir/photo.jpg");
        assertThat(info.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void uploadThenExistsThenDelete() {
        FileStorageManager manager = manager();
        manager.upload(null, "x.txt", new ByteArrayInputStream("v".getBytes(StandardCharsets.UTF_8)), null);

        assertThat(manager.exists(null, "x.txt")).isTrue();

        manager.delete(null, "x.txt");
        assertThat(manager.exists(null, "x.txt")).isFalse();
    }

    @Test
    void generateUrlDelegatesToBackend() {
        FileStorageManager manager = manager();
        manager.upload(null, "y.txt", new ByteArrayInputStream("v".getBytes(StandardCharsets.UTF_8)), null);
        assertThat(manager.generateUrl(null, "y.txt", 0).toString()).endsWith("y.txt");
    }
}
