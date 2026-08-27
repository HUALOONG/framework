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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件扩展点桥接门面：把插件侧的扩展点映射为 core {@link ExtensionLoader} 的动态发现源。
 *
 * <p>扩展点 id 的唯一权威来源是接口上的 {@code @SPI.id()}（缺省 = 接口全限定名）；插件侧
 * {@code @Extension.extensionPoint} 与描述符 {@code extensionPoints[].id} 必须与之一致，
 * 不一致时拒绝注册并 warn，避免配置漂移导致桥接静默失效。
 *
 * <p>同一扩展点可被多个插件共享：注册计引用数，源只创建一次；全部引用注销后才移除共享源。
 * 插件级 API（{@link #registerPlugin} / {@link #unregisterPlugin}）供加载/卸载流程使用，
 * 单点 API（{@link #registerExtensionPoint} / {@link #unregisterExtensionPoint}）供一次性注册使用。
 *
 * @author 王飞
 * @since 2026-08-27
 */
@NullMarked
public final class PluginSpiBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginSpiBridge.class);

    private final ExtensionRegistry registry;
    /**
     * 扩展点 id → 扩展点接口类型（存在即表示源已注册到对应加载器）。
     */
    private final Map<String, Class<?>> mappings = new ConcurrentHashMap<>();
    /**
     * 扩展点 id → 引用该扩展点的注册次数（每注册一次 +1，注销 -1，归零时移除源）。
     */
    private final Map<String, Integer> refCounts = new ConcurrentHashMap<>();
    /**
     * 插件 id → 该插件经桥接注册的扩展点 id 集合，用于批量注销。
     */
    private final Map<String, Set<String>> pluginPoints = new ConcurrentHashMap<>();

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
     * <p>重复注册同一扩展点会累加引用计数，需等量 {@link #unregisterExtensionPoint(String)} 后源才会移除。
     *
     * @param id   扩展点 id（须与 {@code @SPI.id()} 一致，缺省为接口全限定名），不可为空
     * @param type 扩展点接口，不可为 {@code null}；必须为标注 {@code @SPI} 的接口，否则跳过并 warn
     * @return 是否成功注册
     */
    public boolean registerExtensionPoint(String id, Class<?> type) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("扩展点 id 不能为空");
        }
        if (type == null) {
            throw new IllegalArgumentException("扩展点接口不能为 null");
        }
        if (!type.isInterface()) {
            LOGGER.warn("扩展点必须是接口，跳过桥接：" + id + " -> " + type.getName());
            return false;
        }
        if (!type.isAnnotationPresent(SPI.class)) {
            LOGGER.warn("扩展点接口未标注 @SPI，跳过桥接：" + id + " -> " + type.getName());
            return false;
        }
        SPI spi = type.getAnnotation(SPI.class);
        String authoritativeId = spi.id().isBlank() ? type.getName() : spi.id();
        if (!authoritativeId.equals(id)) {
            LOGGER.warn("扩展点 id 与 @SPI.id() 不一致，跳过桥接：声明 " + id + "，权威值 " + authoritativeId);
            return false;
        }
        boolean firstTime = mappings.putIfAbsent(id, type) == null;
        if (firstTime) {
            registerSource(id, type);
        }
        refCounts.merge(id, 1, Integer::sum);
        return true;
    }

    /**
     * 从插件描述符注册扩展点映射。
     *
     * @param descriptor 扩展点描述符，不可为 {@code null}
     * @return 是否成功注册
     */
    public boolean registerExtensionPoint(ExtensionPointDescriptor descriptor) {
        if (descriptor == null) {
            throw new IllegalArgumentException("descriptor cannot be null");
        }
        Class<?> type;
        try {
            type = ClassUtils.forName(descriptor.interfaceName());
        } catch (ClassNotFoundException e) {
            LOGGER.warn("扩展点接口未在应用 classpath 中找到，跳过桥接：" + descriptor.interfaceName());
            return false;
        }
        return registerExtensionPoint(descriptor.id(), type);
    }

    /**
     * 注销一次扩展点引用，引用归零时移除共享源，插件扩展对 core ExtensionLoader 不再可见。
     *
     * @param id 扩展点 id，不可为 {@code null}
     * @return 是否实际移除了源（引用未归零时返回 {@code false}）
     */
    public boolean unregisterExtensionPoint(String id) {
        if (id == null) {
            throw new IllegalArgumentException("扩展点 id 不能为 null");
        }
        if (decrementRef(id) > 0) {
            // 仍有其他注册者引用该扩展点，保留共享源
            return false;
        }
        Class<?> type = mappings.remove(id);
        if (type == null) {
            return false;
        }
        return ExtensionLoader.getExtensionLoader(type).removeSource("bridge:" + id);
    }

    /**
     * 为插件批量注册扩展点映射（每个扩展点引用 +1，并记录该插件注册的扩展点 id 供批量注销）。
     *
     * @param pluginId 插件 id，不可为空
     * @param points   扩展点描述符列表，不可为 {@code null}
     */
    public void registerPlugin(String pluginId, List<ExtensionPointDescriptor> points) {
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalArgumentException("插件 id 不能为空");
        }
        if (points == null) {
            throw new IllegalArgumentException("points cannot be null");
        }
        Set<String> ids = pluginPoints.computeIfAbsent(pluginId, k -> ConcurrentHashMap.newKeySet());
        for (ExtensionPointDescriptor point : points) {
            if (registerExtensionPoint(point)) {
                ids.add(point.id());
            }
        }
    }

    /**
     * 注销插件注册的全部扩展点映射（引用 -1，归零的共享源被移除）。
     *
     * @param pluginId 插件 id，不可为 {@code null}
     */
    public void unregisterPlugin(String pluginId) {
        if (pluginId == null) {
            throw new IllegalArgumentException("插件 id 不能为 null");
        }
        Set<String> ids = pluginPoints.remove(pluginId);
        if (ids == null) {
            return;
        }
        for (String id : ids) {
            unregisterExtensionPoint(id);
        }
    }

    /**
     * 注销全部扩展点映射并重置引用状态（应用/插件容器销毁时调用）。
     */
    public void clear() {
        pluginPoints.clear();
        refCounts.clear();
        for (String id : List.copyOf(mappings.keySet())) {
            unregisterExtensionPoint(id);
        }
    }

    /**
     * 引用计数 -1，返回剩余引用数（无记录视为归零）。
     */
    private int decrementRef(String id) {
        Integer count = refCounts.get(id);
        if (count == null) {
            return 0;
        }
        int remaining = count - 1;
        if (remaining <= 0) {
            refCounts.remove(id);
        } else {
            refCounts.put(id, remaining);
        }
        return remaining;
    }

    /**
     * 为扩展点接口注入插件扩展源。
     */
    private <T> void registerSource(String id, Class<T> type) {
        ExtensionLoader.getExtensionLoader(type)
                .addSource(new PluginExtensionSource<>(registry, id, type));
    }
}