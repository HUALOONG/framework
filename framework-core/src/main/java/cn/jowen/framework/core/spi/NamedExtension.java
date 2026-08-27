package cn.jowen.framework.core.spi;

/**
 * 命名扩展实体：一个扩展实现实例及其元数据。
 *
 * @param name      实现名（对应 {@link SPIImplementation#name()} 或插件扩展 id），不可为空
 * @param instance  实现实例，不可为 {@code null}
 * @param order     排序权重，值越小优先级越高（自动激活集合内生效）
 * @param groups    自动激活分组，空数组表示无分组限制
 * @param activated 是否自动激活（独立字段，不从 groups 推导；空分组实现同样算激活）
 * @param sourceId  来源标识（见 {@link ExtensionSource#sourceId()}）
 * @param <T>       扩展点接口类型
 * @author 王飞
 * @since 2026-08-27
 */
public record NamedExtension<T>(
        String name,
        T instance,
        int order,
        String[] groups,
        boolean activated,
        String sourceId
) {
}