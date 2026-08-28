package cn.jowen.framework.extras.storage.strategy;

import cn.jowen.framework.extras.properties.NamingStrategy;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 原始文件名策略：保留上传时的文件名。
 *
 * <p>注意：该策略在并发上传同名文件时会相互覆盖，生产环境建议使用
 * {@link UuidStrategy} 或 {@link DatePathStrategy}。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class OriginalNameStrategy implements ObjectNameStrategy {

    /**
     * 执行generate操作。
     * @param originalName 参数 originalName
     * @return 结果
     */
    @Override
    public String generate(String originalName, @Nullable byte[] content) {
        return ObjectNames.fileNameOf(originalName);
    }

    /**
     * 执行type操作。
     * @return 结果
     */
    @Override
    public NamingStrategy type() {
        return NamingStrategy.ORIGINAL;
    }
}
