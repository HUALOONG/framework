package cn.jowen.framework.extras.storage.strategy;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * UUID 命名策略：生成 {@code <uuid><扩展名>} 形式对象名（保留原扩展名）。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public final class UuidStrategy implements ObjectNameStrategy {

    static String extension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot > 0 && dot < name.length() - 1) {
            return name.substring(dot);
        }
        return "";
    }

    @Override
    public String generate(@Nullable String originalFilename) {
        String base = DatePathStrategy.basename(originalFilename);
        String ext = extension(base);
        return UUID.randomUUID().toString().replace("-", "") + ext;
    }
}
