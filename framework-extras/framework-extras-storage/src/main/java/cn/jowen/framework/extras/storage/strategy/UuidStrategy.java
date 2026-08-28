package cn.jowen.framework.extras.storage.strategy;

import cn.jowen.framework.extras.properties.NamingStrategy;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * UUID 命名策略：{@code <uuid><扩展名>}，避免同名覆盖与文件名冲突。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class UuidStrategy implements ObjectNameStrategy {

    /**
     * 执行generate操作。
     * @param originalName 参数 originalName
     * @return 结果
     */
    @Override
    public String generate(String originalName, @Nullable byte[] content) {
        return UUID.randomUUID() + ObjectNames.extensionOf(originalName);
    }

    /**
     * 执行type操作。
     * @return 结果
     */
    @Override
    public NamingStrategy type() {
        return NamingStrategy.UUID;
    }
}
