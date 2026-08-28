package cn.jowen.framework.extras.storage.strategy;

import cn.jowen.framework.extras.properties.NamingStrategy;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 日期路径策略：{@code yyyy/MM/dd/<uuid><扩展名>}。
 *
 * <p>按天分目录便于生命周期管理与归档清理，同时用 UUID 避免同日同名冲突。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class DatePathStrategy implements ObjectNameStrategy {

    /** FORMATTER 常量。 */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /**
     * 执行generate操作。
     * @param originalName 参数 originalName
     * @return 结果
     */
    @Override
    public String generate(String originalName, @Nullable byte[] content) {
        return LocalDate.now().format(FORMATTER) + "/" + UUID.randomUUID()
                + ObjectNames.extensionOf(originalName);
    }

    /**
     * 执行type操作。
     * @return 结果
     */
    @Override
    public NamingStrategy type() {
        return NamingStrategy.DATE;
    }
}
