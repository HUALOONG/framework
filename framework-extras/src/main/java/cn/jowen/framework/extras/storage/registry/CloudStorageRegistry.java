package cn.jowen.framework.extras.storage.registry;

import cn.jowen.framework.extras.storage.FileStorage;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * 云存储注册表 SPI 接口。
 *
 * <p>聚合所有已注册的云后端 {@link FileStorage}，支持按名称获取。
 * 由 {@code ExtrasBootstrapConfiguration} 统一注入并调用 {@code manager.registerStorage()} 填充。
 *
 * @author 王飞
 * @since 2026-08-22
 * @see DefaultCloudStorageRegistry
 */
@NullMarked
public interface CloudStorageRegistry {

    /**
     * 按名称获取云存储实例。
     *
     * @param name 存储名称
     * @return 存储实例，不存在返回 {@code null}
     */
    @Nullable
    FileStorage getStorage(String name);

    /**
     * 注册云存储实例。
     *
     * @param name    存储名称
     * @param storage 存储实例
     */
    void registerStorage(String name, FileStorage storage);

    /**
     * 返回所有已注册存储名称。
     *
     * @return 名称集合，不可为 {@code null}
     */
    Set<String> storageNames();
}
