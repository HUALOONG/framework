package cn.jowen.framework.core.spi;

import cn.jowen.framework.core.exception.SystemException;
import cn.jowen.framework.core.util.ClassUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SPI 扩展加载器，按扩展点接口从多个 {@link ExtensionSource} 聚合加载并缓存其实现。
 *
 * <p>默认内置 {@link ServiceLoaderExtensionSource}（META-INF/services 静态发现）；可通过
 * {@link #addSource(ExtensionSource)} 注入动态源（如插件热注册）。同名实现冲突时后注册的源优先，
 * 即动态源可覆盖静态实现。
 *
 * <p>支持：按名获取（{@link #getExtension(String)}）、默认实现（接口 {@link SPI#value()}）、自动激活集合
 * （{@link NamedExtension#activated()}，按 {@code order} 升序）、按源追溯（{@link #getExtensionsBySource(String)}）。
 *
 * @param <T> 扩展点接口类型
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public final class ExtensionLoader<T> {
    /**
     * 已加载的扩展点接口与对应的加载器实例。
     */
    private static final Map<Class<?>, ExtensionLoader<?>> LOADERS = new ConcurrentHashMap<>();

    /**
     * 扩展点接口类型。
     */
    private final Class<T> type;

    /**
     * 扩展发现源列表，首个为默认静态源，动态源追加在后（尾部优先级最高）。
     */
    private final List<ExtensionSource<T>> sources = new CopyOnWriteArrayList<>();

    /**
     * 按实现名聚合的扩展缓存，源变更（注册/移除/内容变化）时置空。
     */
    private volatile @Nullable Map<String, NamedExtension<T>> namedCache;

    /**
     * 自动激活扩展缓存（按 order 升序），源变更时置空。
     */
    private volatile @Nullable List<NamedExtension<T>> activatedCache;

    /**
     * 创建扩展点加载器。
     *
     * @param type 扩展点接口类型
     */
    private ExtensionLoader(Class<T> type) {
        this.type = type;
        this.sources.add(new ServiceLoaderExtensionSource<>());
    }

    /**
     * 获取（或创建并缓存）指定扩展点接口的加载器。
     *
     * @param type 扩展点接口，不可为 {@code null}；必须为接口
     * @param <T>  类型
     * @return 加载器实例，不可为 {@code null}
     */
    @SuppressWarnings("unchecked")
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        if (!type.isInterface()) {
            throw new SystemException("SPI 扩展点必须是接口：" + type);
        }
        return (ExtensionLoader<T>) LOADERS.computeIfAbsent(type, ExtensionLoader::new);
    }

    /**
     * 判断数组中是否包含指定值。
     *
     * @param arr   数组
     * @param value 值
     * @return 是否包含
     */
    private static boolean contains(String[] arr, String value) {
        for (String s : arr) {
            if (s.equals(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 添加扩展发现源（插件装载时调用）。同名实现冲突时后注册的源优先。
     *
     * @param source 扩展源，不可为 {@code null}
     * @throws SystemException source 为 {@code null} 时抛出
     */
    public void addSource(ExtensionSource<T> source) {
        if (source == null) {
            throw new SystemException("扩展源不能为 null：" + type.getName());
        }
        source.addChangeListener(this::invalidateCache);
        sources.add(source);
        invalidateCache();
    }

    /**
     * 移除指定标识的扩展发现源（插件卸载时调用）。
     *
     * @param sourceId 源标识，不可为 {@code null}
     * @return 是否成功移除
     * @throws SystemException sourceId 为 {@code null} 时抛出
     */
    public boolean removeSource(String sourceId) {
        if (sourceId == null) {
            throw new SystemException("sourceId 不能为 null");
        }
        boolean removed = sources.removeIf(s -> s.sourceId().equals(sourceId));
        if (removed) {
            invalidateCache();
        }
        return removed;
    }

    /**
     * 刷新全部扩展源的缓存（热部署场景手动兜底；动态源经变更回调自动失效，通常无需调用）。
     */
    public void refreshDynamicSources() {
        invalidateCache();
    }

    /**
     * 按实现名获取扩展实例。
     *
     * @param name 实现名，对应 {@link SPIImplementation#name()} 或插件扩展 id
     * @return 扩展实例，不可为 {@code null}
     * @throws SystemException 名称为空或未找到对应实现时抛出
     */
    public T getExtension(String name) {
        if (name.isBlank()) {
            throw new SystemException("扩展名不能为空");
        }
        NamedExtension<T> ext = loadAll().get(name);
        if (ext != null) {
            return ext.instance();
        }
        throw new SystemException("未找到 SPI 实现：" + type.getName() + " -> " + name);
    }

    /**
     * 获取默认扩展实例（依据接口 {@link SPI#value()}）。
     *
     * @return 默认实例，不可为 {@code null}
     * @throws SystemException 未声明默认实现时抛出
     */
    public T getDefaultExtension() {
        SPI spi = type.getAnnotation(SPI.class);
        if (spi == null || spi.value().isBlank()) {
            throw new SystemException("扩展点未声明默认实现：" + type.getName());
        }
        return getExtension(spi.value());
    }

    /**
     * 获取全部自动激活扩展实例，按 order 升序排列；可按分组过滤。
     *
     * <p>分组规则：候选分组为空（无分组限制）时对任意分组查询均放行；否则需与任一请求分组匹配。
     *
     * @param groups 分组过滤，为空表示返回全部激活实例
     * @return 已排序的激活实例列表，不可为 {@code null}
     */
    public List<T> getActivateExtensions(@Nullable String... groups) {
        List<NamedExtension<T>> all = loadActivate();
        if (groups.length == 0) {
            return all.stream().map(NamedExtension::instance).toList();
        }
        List<T> filtered = new ArrayList<>();
        for (NamedExtension<T> ext : all) {
            String[] gs = ext.groups();
            if (gs.length == 0) {
                filtered.add(ext.instance());
                continue;
            }
            for (String g : groups) {
                if (g != null && contains(gs, g)) {
                    filtered.add(ext.instance());
                    break;
                }
            }
        }
        return filtered;
    }

    /**
     * 获取指定扩展点的全部实现（不论是否激活），含静态与动态源。
     *
     * @return 实现列表，不可为 {@code null}
     */
    public List<T> getAllExtensions() {
        return loadAll().values().stream().map(NamedExtension::instance).toList();
    }

    /**
     * 按源标识获取扩展元数据（调试/监控用）。
     *
     * @param sourceId 源标识，不可为 {@code null}
     * @return 该源提供的命名扩展列表，不可为 {@code null}（可为空）
     * @throws SystemException sourceId 为 {@code null} 时抛出
     */
    public List<NamedExtension<T>> getExtensionsBySource(String sourceId) {
        if (sourceId == null) {
            throw new SystemException("sourceId 不能为 null");
        }
        return loadAll().values().stream()
                .filter(ext -> ext.sourceId().equals(sourceId))
                .toList();
    }

    /**
     * 从所有源聚合扩展为按名索引，后注册的源覆盖同名实现。
     *
     * @return 按名索引的扩展映射，不可为 {@code null}
     */
    private Map<String, NamedExtension<T>> loadAll() {
        Map<String, NamedExtension<T>> cached = namedCache;
        if (cached != null) {
            return cached;
        }
        Map<String, NamedExtension<T>> merged = new LinkedHashMap<>();
        ClassLoader cl = ClassUtils.getDefaultClassLoader();
        for (ExtensionSource<T> source : sources) {
            for (NamedExtension<T> ext : source.load(type, cl)) {
                merged.put(ext.name(), ext);
            }
        }
        namedCache = merged;
        return merged;
    }

    /**
     * 加载并缓存全部自动激活扩展（按 order 升序）。
     *
     * @return 已排序的激活扩展列表，不可为 {@code null}
     */
    private List<NamedExtension<T>> loadActivate() {
        List<NamedExtension<T>> cached = activatedCache;
        if (cached != null) {
            return cached;
        }
        List<NamedExtension<T>> result = new ArrayList<>();
        for (NamedExtension<T> ext : loadAll().values()) {
            if (ext.activated()) {
                result.add(ext);
            }
        }
        result.sort(Comparator.comparingInt(NamedExtension::order));
        activatedCache = result;
        return result;
    }

    /**
     * 失效全部缓存，下次查询从各源重新加载。
     */
    private void invalidateCache() {
        namedCache = null;
        activatedCache = null;
    }
}