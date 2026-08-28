package cn.jowen.framework.cache.api;

import org.jspecify.annotations.NullMarked;

/**
 * 空值标记对象，用于缓存穿透防护。当数据源不存在某 key 时，
 * 将本对象写入缓存以占用占位，避免重复击穿到数据源。
 * 判断时通过 {@code instanceOf NullValue} 而非 {@code == null} 区分"未命中"与"明确为空"。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class NullValue {

    /**
     * 唯一实例，单例复用。
     */
    public static final NullValue INSTANCE = new NullValue();

    private NullValue() {
    }

    @Override
    public String toString() {
        return "NullValue{}";
    }
}
