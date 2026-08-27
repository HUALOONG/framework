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
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SPI 扩展加载器，按扩展点接口加载并缓存其实现。
 *
 * <p>支持：按名获取（{@link #getExtension(String)}）、默认实现（接口 {@link SPI#value()}）、自动激活集合（带 {@link Activate} 的实现，按权重排序）。
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
     * 已加载的扩展点实现。
     */
    private final Map<String, T> namedInstances = new ConcurrentHashMap<>();

    /**
     * 默认加载的扩展点实现。
     */
    private final Map<String, T> defaultInstances = new ConcurrentHashMap<>();

    /**
     * 已加载的自动激活扩展点实现。
     */
    private volatile @Nullable List<T> activatedCache;

    /**
     * 创建扩展点加载器。
     *
     * @param type 扩展点接口类型
     */
    private ExtensionLoader(Class<T> type) {
        this.type = type;
    }

    /**
     * 获取（或创建并缓存）指定扩展点接口的加载器。
     *
     * @param type 扩展点接口，不可为 {@code null}
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
     * 按实现名获取扩展实例（懒加载并缓存）。
     *
     * @param name 实现名，对应 {@link SPIImplementation#name()}
     * @return 扩展实例，不可为 {@code null}
     * @throws SystemException 未找到对应实现时抛出
     */
    public T getExtension(String name) {
        if (name.isBlank()) {
            throw new SystemException("扩展名不能为空");
        }
        T instance = namedInstances.get(name);
        if (instance != null) {
            return instance;
        }
        for (T candidate : ServiceLoader.load(type, ClassUtils.getDefaultClassLoader())) {
            SPIImplementation impl = candidate.getClass().getAnnotation(SPIImplementation.class);
            if (impl != null && name.equals(impl.name())) {
                T prev = namedInstances.putIfAbsent(name, candidate);
                return prev != null ? prev : candidate;
            }
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
     * 获取所有被 {@link Activate} 标记且（可选）匹配分组的自动激活实例，按权重升序排列。
     *
     * @param groups 分组过滤，为空表示返回全部激活实例
     * @return 已排序的激活实例列表，不可为 {@code null}
     */
    public List<T> getActivateExtensions(@Nullable String... groups) {
        List<T> all = loadActivate();
        if (groups.length == 0) {
            return all;
        }
        List<T> filtered = new ArrayList<>();
        for (T ext : all) {
            Activate activate = ext.getClass().getAnnotation(Activate.class);
            if (activate == null) {
                continue;
            }
            if (activate.group().length == 0) {
                filtered.add(ext);
                continue;
            }
            for (String g : groups) {
                if (g != null && contains(activate.group(), g)) {
                    filtered.add(ext);
                    break;
                }
            }
        }
        return filtered;
    }

    /**
     * 加载并缓存所有被 {@link Activate} 标记的自动激活实例。
     *
     * @return 已排序的激活实例列表，不可为 {@code null}
     */
    private List<T> loadActivate() {
        List<T> cached = activatedCache;
        if (cached != null) {
            return cached;
        }
        Map<Integer, T> ordered = new LinkedHashMap<>();
        List<T> result = new ArrayList<>();
        for (T candidate : ServiceLoader.load(type, ClassUtils.getDefaultClassLoader())) {
            if (candidate.getClass().isAnnotationPresent(Activate.class)) {
                result.add(candidate);
            }
        }
        result.sort(Comparator.comparingInt(e -> {
            Activate a = e.getClass().getAnnotation(Activate.class);
            return a != null ? a.order() : 0;
        }));
        activatedCache = result;
        return result;
    }
}
