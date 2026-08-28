package cn.jowen.framework.core.spi;

import java.util.List;

/**
 * 扩展发现源：为 {@link ExtensionLoader} 提供扩展实现的抽象。
 *
 * <p>内置实现：
 * <ul>
 *   <li>{@link ServiceLoaderExtensionSource} — META-INF/services 静态发现（加载器的默认源）</li>
 * </ul>
 *
 * <p>外部实现（framework-plugin 层提供）：从 {@code ExtensionRegistry} 动态读取插件扩展，支持热注册/注销。
 *
 * @param <T> 扩展点接口类型
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
public interface ExtensionSource<T> {

    /**
     * 此源的唯一标识（用于 {@link ExtensionLoader#removeSource(String)} 与日志）。
     *
     * @return 源标识，不可为 {@code null}
     */
    String sourceId();

    /**
     * 加载此源能提供的全部扩展实现。
     *
     * @param extensionPoint 扩展点接口类型，不可为 {@code null}
     * @param classLoader    建议使用的类加载器，源可忽略并使用自身的隔离类加载器
     * @return 命名扩展列表，不可为 {@code null}（可为空）
     */
    List<NamedExtension<T>> load(Class<T> extensionPoint, ClassLoader classLoader);

    /**
     * 注册内容变更回调：源内扩展发生增删（如插件热注册/注销）时调用，以触发加载器缓存失效。
     *
     * @param listener 变更回调，不可为 {@code null}
     */
    default void addChangeListener(Runnable listener) {
        // 静态源内容不可变，无需响应
    }
}