package cn.jowen.framework.extras.storage;

import cn.jowen.framework.extras.storage.exception.StorageException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件存储管理器：统一多存储桶 / 多后端的文件操作门面。
 *
 * <p>维护 {@code 桶名 -> FileStorage} 映射，支持按桶选择后端；未指定桶时使用默认桶。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class FileStorageManager {

    /** storages 不可变字段。 */
    private final Map<String, FileStorage> storages = new ConcurrentHashMap<>();
    /** defaultBucket 不可变字段。 */
    private final String defaultBucket;

    /**
     * 构造实例。
     * @param defaultBucket 参数 defaultBucket
     */
    public FileStorageManager(Map<String, FileStorage> storages, String defaultBucket) {
        this.storages.putAll(storages);
        this.defaultBucket = defaultBucket;
    }

    /**
     * 获取指定桶的存储实现。
     *
     * @param bucket 桶名，空则取默认桶
     * @return 存储实现
     * @throws StorageException 桶不存在时抛出
     */
    public FileStorage getStorage(@Nullable String bucket) {
        String name = (bucket == null || bucket.isBlank()) ? defaultBucket : bucket;
        FileStorage storage = storages.get(name);
        if (storage == null) {
            throw new StorageException("未注册的存储桶: " + name);
        }
        return storage;
    }

    /** @return 默认存储实现 */
    public FileStorage getDefault() {
        return getStorage(null);
    }

    /**
     * 上传文件。
     *
     * @param bucket      桶名
     * @param key         对象键
     * @param content     文件流
     * @param contentType 内容类型
     * @return 文件元信息
     */
    public FileInfo upload(@Nullable String bucket, String key, InputStream content,
                           @Nullable String contentType) {
        FileStorage storage = getStorage(bucket);
        String storedKey = storage.put(key, content, contentType);
        return new FileInfo(resolveBucket(bucket), fileNameOf(key), storedKey,
                storage.generateUrl(storedKey, 0).toString(), null, contentType, -1L,
                System.currentTimeMillis());
    }

    /**
     * 删除文件。
     *
     * @param bucket 桶名
     * @param key    对象键
     */
    public void delete(@Nullable String bucket, String key) {
        getStorage(bucket).delete(key);
    }

    /**
     * 生成访问 URL。
     *
     * @param bucket 桶名
     * @param key    对象键
     * @param expire 有效期（毫秒）
     * @return 访问 URL
     */
    public URL generateUrl(@Nullable String bucket, String key, long expire) {
        return getStorage(bucket).generateUrl(key, expire);
    }

    /**
     * 判断文件是否存在。
     *
     * @param bucket 桶名
     * @param key    对象键
     * @return 是否存在
     */
    public boolean exists(@Nullable String bucket, String key) {
        return getStorage(bucket).exists(key);
    }

    private String resolveBucket(@Nullable String bucket) {
        return (bucket == null || bucket.isBlank()) ? defaultBucket : bucket;
    }

    private static String fileNameOf(String key) {
        int idx = key.lastIndexOf('/');
        return idx < 0 ? key : key.substring(idx + 1);
    }
}
