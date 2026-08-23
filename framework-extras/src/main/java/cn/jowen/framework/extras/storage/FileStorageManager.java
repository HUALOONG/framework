package cn.jowen.framework.extras.storage;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 文件存储器管理器：维护命名存储实例，支持按名称获取与注册。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public interface FileStorageManager {

    /**
     * 获取默认存储实例。
     */
    FileStorage getStorage();

    /**
     * 按名称获取存储实例。
     *
     * @param name 存储名称
     * @return 存储实例，不存在返回 {@code null}
     */
    @Nullable
    FileStorage getStorage(String name);

    /**
     * 注册存储实例。
     *
     * @param name    存储名称
     * @param storage 存储实例
     */
    void registerStorage(String name, FileStorage storage);
}
