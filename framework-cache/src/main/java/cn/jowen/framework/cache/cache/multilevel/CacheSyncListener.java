package cn.jowen.framework.cache.cache.multilevel;

import org.jspecify.annotations.NullMarked;

/**
 * 多级缓存跨节点同步监听器，接收远端 put/evict 事件以保持本地缓存一致。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface CacheSyncListener {

    void onCachePut(String key, Object value);

    void onCacheEvict(String key);
}
