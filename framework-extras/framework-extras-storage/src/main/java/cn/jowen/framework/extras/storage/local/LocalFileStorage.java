package cn.jowen.framework.extras.storage.local;

import cn.jowen.framework.extras.storage.FileStorage;
import cn.jowen.framework.extras.properties.StorageType;
import cn.jowen.framework.extras.storage.exception.StorageException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 基于本地文件系统的 {@link FileStorage} 实现。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class LocalFileStorage implements FileStorage {

    private final Path root;

    public LocalFileStorage(String baseDir) {
        this.root = Paths.get(baseDir);
    }

    @Override
    public StorageType type() {
        return StorageType.LOCAL;
    }

    @Override
    public String put(String key, InputStream content, @Nullable String contentType) {
        try {
            Path target = root.resolve(key).normalize();
            if (!target.startsWith(root)) {
                throw new StorageException("非法存储路径: " + key);
            }
            Files.createDirectories(target.getParent());
            Files.copy(content, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return key;
        } catch (IOException e) {
            throw new StorageException("本地文件写入失败: " + key, e);
        }
    }

    @Override
    public @Nullable InputStream get(String key) {
        try {
            Path target = resolve(key);
            if (!Files.exists(target)) {
                return null;
            }
            return Files.newInputStream(target);
        } catch (IOException e) {
            throw new StorageException("本地文件读取失败: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new StorageException("本地文件删除失败: " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        return Files.exists(resolve(key));
    }

    @Override
    public URL generateUrl(String key, long expire) {
        try {
            return resolve(key).toUri().toURL();
        } catch (Exception e) {
            throw new StorageException("生成访问 URL 失败: " + key, e);
        }
    }

    private Path resolve(String key) {
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new StorageException("非法存储路径: " + key);
        }
        return target;
    }
}
