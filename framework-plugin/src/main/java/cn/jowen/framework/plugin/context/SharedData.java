package cn.jowen.framework.plugin.context;

import org.jspecify.annotations.NullMarked;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * 插件间共享数据。基于 {@link java.util.concurrent.ConcurrentHashMap} 实现，线程安全。
 *
 * @author 王飞
 */
@NullMarked
public final class SharedData implements Map<String, Object> {
    /**
     * 底层存储
     */
    private final Map<String, Object> delegate = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return delegate.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        return delegate.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return delegate.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
        delegate.putAll(m);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public Set<String> keySet() {
        return delegate.keySet();
    }

    @Override
    public Collection<Object> values() {
        return delegate.values();
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        return delegate.entrySet();
    }

    /**
     * 安全获取并类型转换。
     */
    @SuppressWarnings("unchecked")
    public <T> T getTyped(String key) {
        return (T) delegate.get(key);
    }

    /**
     * 安全放入。
     */
    public void putTyped(String key, Object value) {
        delegate.put(key, value);
    }
}
