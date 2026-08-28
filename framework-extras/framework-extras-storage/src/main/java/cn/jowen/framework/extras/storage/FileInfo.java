package cn.jowen.framework.extras.storage;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 文件元信息。
 *
 * @param bucket      桶名
 * @param name        文件名（原始名）
 * @param key         对象键（路径）
 * @param url         访问地址
 * @param hash        内容哈希（可为 {@code null}）
 * @param contentType 内容类型（可为 {@code null}）
 * @param size        文件大小（字节），未知为 -1
 * @param uploadTime  上传时间戳（毫秒）
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public record FileInfo(
        String bucket,
        String name,
        String key,
        String url,
        @Nullable String hash,
        @Nullable String contentType,
        long size,
        long uploadTime) {
}
