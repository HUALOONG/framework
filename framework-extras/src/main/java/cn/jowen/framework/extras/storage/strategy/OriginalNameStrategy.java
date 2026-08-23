package cn.jowen.framework.extras.storage.strategy;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 原名命名策略：保留原始文件名（去除路径分隔符），适合已保证唯一性的场景。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public final class OriginalNameStrategy implements ObjectNameStrategy {

    @Override
    public String generate(@Nullable String originalFilename) {
        return DatePathStrategy.basename(originalFilename);
    }
}
