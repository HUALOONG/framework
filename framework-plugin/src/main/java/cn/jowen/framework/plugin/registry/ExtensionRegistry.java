package cn.jowen.framework.plugin.registry;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 扩展点注册表。
 *
 * @author 王飞
 */
@NullMarked
public final class ExtensionRegistry {

    /**
     * 扩展点 id → 扩展列表。
     */
    private final Map<String, List<Extension>> registry = new ConcurrentHashMap<>();

    /**
     * 注册表内容变更监听（注册/注销/清空触发），供桥接层做缓存失效。
     */
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    /**
     * 注册扩展实现。
     *
     * @param extension 扩展实现，不可为 {@code null}
     * @throws IllegalArgumentException 扩展点已存在相同 id 时抛出
     */
    public void register(Extension extension) {
        if (extension == null) throw new IllegalArgumentException("extension cannot be null");
        List<Extension> list = registry.computeIfAbsent(extension.extensionPointId(), k -> new ArrayList<>());
        // 唯一性校验
        for (Extension existing : list) {
            if (existing.id().equals(extension.id())) {
                throw new IllegalArgumentException("扩展实现 id 重复：" + extension.id());
            }
        }
        list.add(extension);
        // 按 order 排序
        list.sort(Comparator.comparingInt(Extension::order));
        notifyChanged();
    }

    /**
     * 注销扩展实现。
     *
     * @param extensionPointId 扩展点 id，不可为 {@code null}
     * @param extensionId      扩展实现 id，不可为 {@code null}
     * @return 被注销的扩展，不存在时返回 {@code null}
     */
    public @Nullable Extension unregister(String extensionPointId, String extensionId) {
        List<Extension> list = registry.get(extensionPointId);
        if (list == null) return null;
        Extension removed = null;
        for (Extension ext : list) {
            if (ext.id().equals(extensionId)) {
                removed = ext;
                break;
            }
        }
        if (removed != null) {
            list.remove(removed);
            notifyChanged();
        }
        return removed;
    }

    /**
     * 按扩展点 id 获取所有扩展实现（按 order 升序）。
     *
     * @param extensionPointId 扩展点 id，不可为 {@code null}
     * @return 扩展列表，不可为 {@code null}（可能为空）
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getExtensions(String extensionPointId) {
        List<Extension> list = registry.get(extensionPointId);
        if (list == null || list.isEmpty()) return List.of();
        return (List<T>) (List<?>) list;
    }

    /**
     * 按扩展点接口类型获取所有扩展实现。
     *
     * @param extensionPointClass 扩展点接口类型，不可为 {@code null}
     * @param <T>                 扩展点接口类型
     * @return 扩展列表，不可为 {@code null}（可能为空）
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getExtensionsByType(Class<T> extensionPointClass) {
        List<T> result = new ArrayList<>();
        for (List<Extension> list : registry.values()) {
            for (Extension ext : list) {
                try {
                    Class<?> clazz = ext.instance().getClass();
                    // 通过类加载器加载接口
                    ClassLoader cl = extensionPointClass.getClassLoader();
                    if (cl != null && cl.loadClass(extensionPointClass.getName()).isAssignableFrom(clazz)) {
                        result.add((T) ext.instance());
                    }
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
        result.sort(Comparator.comparingInt(ext -> {
            // 从扩展列表中找 order
            return 0;
        }));
        return result;
    }

    /**
     * 获取所有扩展点 id。
     */
    public List<String> getExtensionPointIds() {
        return List.copyOf(registry.keySet());
    }

    /**
     * 清空注册表。
     */
    public void clear() {
        registry.clear();
        notifyChanged();
    }

    /**
     * 添加注册表内容变更监听。扩展注册/注销/清空时回调，用于桥接层触发扩展加载器缓存失效。
     *
     * @param listener 变更监听，不可为 {@code null}
     */
    public void addChangeListener(Runnable listener) {
        if (listener == null) throw new IllegalArgumentException("listener cannot be null");
        changeListeners.add(listener);
    }

    /**
     * 通知全部变更监听。
     */
    private void notifyChanged() {
        for (Runnable listener : changeListeners) {
            try {
                listener.run();
            } catch (Exception ignored) {
                // 监听失败不影响注册表自身操作
            }
        }
    }
}
