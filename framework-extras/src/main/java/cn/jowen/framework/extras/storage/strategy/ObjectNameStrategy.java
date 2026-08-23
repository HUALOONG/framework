package cn.jowen.framework.extras.storage.strategy;

import org.jspecify.annotations.NullMarked;

/**
 * 对象命名策略：将原始文件名映射为存储对象名（可含层级路径）。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public interface ObjectNameStrategy {

    /**
     * 生成对象名。
     *
     * @param originalFilename 原始文件名（可能为 null 或含路径）
     * @return 对象名，不可为 {@code null}
     */
    String generate(String originalFilename);
}
