package cn.jowen.framework.plugin.spi;

import cn.jowen.framework.core.spi.ExtensionLoader;
import cn.jowen.framework.core.spi.SPI;
import cn.jowen.framework.core.util.ClassUtils;
import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import cn.jowen.framework.plugin.descriptor.ExtensionPointDescriptor;
import cn.jowen.framework.plugin.registry.ExtensionRegistry;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件扩展点桥接门面：把插件侧的扩展点映射为 core {@link ExtensionLoader} 的动态发现源。
 *
 * <p>扩展点 id 的唯一权威来源是接口上的 {@code @SPI.id()}（缺省 = 接口全限定名）；插件侧
 * {@code @Extension.extensionPoint} 与描述符 {@code extensionPoints[].id} 必须与之一致，
 * 不一致时拒绝注册并 warn，避免配置漂移导致桥接静默失效。
 *
 * <p>用法（boot 环境由 PluginAutoConfiguration 装配）：桥接对象创建后，在插件加载时逐条调用
 * {@link #registerExtensionPoint(String, Class)} 或 {@link #registerExtensionPoint(ExtensionPointDescriptor)}；
 * 插件卸载时调用 {@link #unregisterExtensionPoint(String)}，应用销毁时调用 {@link #clear()}。
 *
 * @author 王飞
 * @since 2026-08-27
 */
@NullMarked
public final class PluginSpiBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginSpiBridge.class);

    private final ExtensionRegistry registry;
    private final Map<String, Class<?>> mappings = new ConcurrentHashMap<>();

    /**
     * 创建桥接门面。
     *
     * @param registry 插件扩展注册表，不可为 {@code null}
     */
    public PluginSpiBridge(ExtensionRegistry registry) {
        if (registry == null) {
            throw new IllegalArgumentException("registry cannot be null");
        }
        this.registry = registry;
    }

    /**
     * 注册扩展点映射，使该扩展点下的插件扩展对 core ExtensionLoader 可见。
     *
     * @param id   扩展点 id（须与 {@code @SPI.id()} 一致，缺省为接口全限定名），不可为空
     * @param type 扩展点接口，不可为 {@code null}；必须为标注 {@code @SPI} 的接口，否则跳过并 warn
     */
    public void registerExtensionPoint(String id, Class<?> type) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("扩展点 id 不能为空");
        }
        if (type == null) {
            throw new IllegalArgumentException("扩展点接口不能为 null");
        }
        if (!type.isInterface()) {
            LOGGER.warn("扩展点必须是接口，跳过桥接：" + id + " -> " + type.getName());
            return;
        }
        if (!type.isAnnotationPresent(SPI.class)) {
            LOGGER.warn("扩展点接口未标注 @SPI，跳过桥接：" + id + " -> " + type.getName());
            return;
        }
        SPI spi = type.getAnnotation(SPI.class);
        String authoritativeId = spi.id().isBlank() ? type.getName() : spi.id();
        if (!authoritativeId.equals(id)) {
            LOGGER.warn("扩展点 id 与 @SPI.id() 不一致，跳过桥接：声明 " + id + "，权威值 " + authoritativeId);
            return;
        }
        registerInternal(id, type);
    }

    /**
     * 从插件描述符注册扩展点映射。
     *
     * @param descriptor 扩展点描述符，不可为 {@code null}
     */
    public void registerExtensionPoint(ExtensionPointDescriptor descriptor) {
        if (descriptor == null) {
            throw new IllegalArgumentException("descriptor cannot be null");
        }
        Class<?> type;
        try {
            type = ClassUtils.forName(descriptor.interfaceName());
        } catch (ClassNotFoundException e) {
            LOGGER.warn("扩展点接口未在应用 classpath 中找到，跳过桥接：" + descriptor.interfaceName());
            return;
        }
        registerExtensionPoint(descriptor.id(), type);
    }

    /**
     * 注销扩展点映射，插件扩展对 core ExtensionLoader 不再可见。
     *
     * @param id 扩展点 id，不可为 {@code null}
     * @return 是否存在并成功移除
     */
    public boolean unregisterExtensionPoint(String id) {
        if (id == null) {
            throw new IllegalArgumentException("扩展点 id 不能为 null");
        }
        Class<?> type = mappings.remove(id);
        if (type == null) {
            return false;
        }
        return ExtensionLoader.getExtensionLoader(type).removeSource("bridge:" + id);
    }

    /**
     * 注销全部扩展点映射（应用/插件容器销毁时调用）。
     */
    public void clear() {
        for (String id : List.copyOf(mappings.keySet())) {
            unregisterExtensionPoint(id);
        }
    }

    /**
     * 为扩展点接口注入插件扩展源并记录映射（幂等，同 id 重复注册直接替换）。
     */
    private <T> void registerInternal(String id, Class<T> type) {
        ExtensionLoader.getExtensionLoader(type)
                .addSource(new PluginExtensionSource<>(registry, id, type));
        mappings.put(id, type);
    }
}