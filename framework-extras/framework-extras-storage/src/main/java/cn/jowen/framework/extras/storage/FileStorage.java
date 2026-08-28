package cn.jowen.framework.extras.storage;

import cn.jowen.framework.extras.properties.StorageType;
import cn.jowen.framework.extras.storage.exception.StorageException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.net.URL;

/**
 * 统一文件存储抽象，屏蔽本地磁盘、阿里云 OSS、AWS S3 等底层差异。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface FileStorage {

    /**
     * 存储类型标识。
     *
     * @return 类型
     */
    StorageType type();

    /**
     * 上传文件。
     *
     * @param key         对象键（路径）
     * @param content     文件流
     * @param contentType 内容类型，可为 {@code null}
     * @return 可访问的存储键
     * @throws StorageException 上传失败时抛出
     */
    String put(String key, InputStream content, @Nullable String contentType);

    /**
     * 下载文件。
     *
     * @param key 对象键
     * @return 文件流，不存在时返回 {@code null}
     * @throws StorageException 下载失败时抛出
     */
    @Nullable InputStream get(String key);

    /**
     * 删除文件。
     *
     * @param key 对象键
     * @throws StorageException 删除失败时抛出
     */
    void delete(String key);

    /**
     * 判断文件是否存在。
     *
     * @param key 对象键
     * @return 是否存在
     */
    boolean exists(String key);

    /**
     * 生成临时访问 URL（如有签名有效期，由实现决定）。
     *
     * @param key    对象键
     * @param expire 有效期（毫秒），小于等于 0 表示默认
     * @return 可访问 URL
     * @throws StorageException 生成失败时抛出
     */
    URL generateUrl(String key, long expire);
}
