package cn.jowen.framework.plugin.spi;

import cn.jowen.framework.core.spi.ExtensionSource;
import cn.jowen.framework.core.spi.NamedExtension;
import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import cn.jowen.framework.plugin.registry.Extension;
import cn.jowen.framework.plugin.registry.ExtensionRegistry;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;

/**
 * 插件扩展源：将 {@link ExtensionRegistry} 中某扩展点的动态扩展桥接为 core {@code ExtensionLoader} 的发现源。
 *
 * <p>数据实时读取注册表：扩展热注册/注销时经 {@link ExtensionRegistry#addChangeListener} 触发
 * 加载器缓存失效，无需手动刷新。
 *
 * @param <T> 扩展点接口类型
 * @author 王飞
 * @since 2026-08-27
 */
@NullMarked
public final class PluginExtensionSource<T> implements ExtensionSource<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginExtensionSource.class);

    private final ExtensionRegistry registry;
    private final String extensionPointId;
    private final Class<T> type;

    /**
     * 创建插件扩展源。
     *
     * @param registry         扩展注册表，不可为 {@code null}
     * @param extensionPointId 扩展点 id（对应 core {@code @SPI.id()}，缺省为接口全限定名），不可为空
     * @param type             扩展点接口类型，不可为 {@code null}
     */
    public PluginExtensionSource(ExtensionRegistry registry, String extensionPointId, Class<T> type) {
        if (registry == null) {
            throw new IllegalArgumentException("registry cannot be null");
        }
        if (extensionPointId == null || extensionPointId.isBlank()) {
            throw new IllegalArgumentException("extensionPointId cannot be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        this.registry = registry;
        this.extensionPointId = extensionPointId;
        this.type = type;
    }

    @Override
    public String sourceId() {
        return "bridge:" + extensionPointId;
    }

    @Override
    public List<NamedExtension<T>> load(Class<T> extensionPoint, ClassLoader classLoader) {
        List<NamedExtension<T>> result = new ArrayList<>();
        // 显式类型局部变量：getExtensions(String) 是泛型方法，增强 for 的目标类型推导会解析为 Object
        List<Extension> extensions = registry.getExtensions(extensionPointId);
        for (Extension ext : extensions) {
            if (!type.isInstance(ext.instance())) {
                LOGGER.warn("扩展实现类型不符，跳过：扩展点 " + extensionPointId + "，扩展 " + ext.id());
                continue;
            }
            @SuppressWarnings("unchecked")
            T instance = (T) ext.instance();
            // 插件扩展默认自动激活、无分组（与 @Activate 空分组语义一致：任意分组查询均放行）
            result.add(new NamedExtension<>(ext.id(), instance, ext.order(),
                    new String[0], true, sourceId()));
        }
        return result;
    }

    @Override
    public void addChangeListener(Runnable listener) {
        // 注册表内容变化即本源内容变化，直接透传，由加载器注入其缓存失效回调
        registry.addChangeListener(listener);
    }
}