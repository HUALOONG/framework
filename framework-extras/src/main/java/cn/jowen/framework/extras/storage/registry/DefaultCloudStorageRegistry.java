package cn.jowen.framework.extras.storage.registry;

import cn.jowen.framework.extras.storage.FileStorage;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 云存储注册表默认实现。
 *
 * <p>使用 {@link ConcurrentHashMap} 保证并发安全；{@code registerStorage} 覆盖同名存储。
 *
 * @author 王飞
 * @since 2026-08-22
 * @see CloudStorageRegistry
 */
@NullMarked
public final class DefaultCloudStorageRegistry implements CloudStorageRegistry {

    private final Map<String, FileStorage> storages = new ConcurrentHashMap<>();

    @Override
    public @Nullable FileStorage getStorage(String name) {
        return storages.get(name);
    }

    @Override
    public void registerStorage(String name, FileStorage storage) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(storage, "storage must not be null");
        storages.put(name, storage);
    }

    @Override
    public Set<String> storageNames() {
        return Set.copyOf(storages.keySet());
    }
}
