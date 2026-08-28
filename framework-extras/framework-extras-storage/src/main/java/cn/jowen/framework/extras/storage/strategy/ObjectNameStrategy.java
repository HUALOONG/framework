package cn.jowen.framework.extras.storage.strategy;

import cn.jowen.framework.extras.properties.NamingStrategy;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 对象命名策略：决定上传文件在存储后端中的对象键（{@code objectName}）。
 *
 * <p>框架内置四种策略，由 {@link ObjectNameStrategyFactory} 按
 * {@link NamingStrategy} 枚举生产：
 * <ul>
 *   <li>{@link OriginalNameStrategy}：保留原始文件名；</li>
 *   <li>{@link UuidStrategy}：UUID + 原始扩展名；</li>
 *   <li>{@link DatePathStrategy}：{@code yyyy/MM/dd} 日期目录 + UUID；</li>
 *   <li>{@link HashStrategy}：内容 MD5 摘要分片 + 扩展名。</li>
 * </ul>
 *
 * <p>业务方实现本接口并注册为 Bean，即可接入自定义命名规则。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface ObjectNameStrategy {

    /**
     * 生成对象键。
     *
     * @param originalName 原始文件名（可含路径，仅取末段）
     * @param content      文件内容；为 {@code null} 时（如流式上传无法预读）
     *                     实现应降级为不依赖内容的策略
     * @return 对象键，不含前导斜杠
     */
    String generate(String originalName, @Nullable byte[] content);

    /** @return 对应的策略枚举 */
    NamingStrategy type();
}
