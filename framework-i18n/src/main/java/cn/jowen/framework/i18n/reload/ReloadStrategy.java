package cn.jowen.framework.i18n.reload;

import org.jspecify.annotations.NullMarked;

/**
 * 资源热加载策略。
 *
 * <p>各策略适用场景：
 * <ul>
 *   <li>{@link #POLLING}：定时轮询变更版本号（数据库场景，默认 30s）；</li>
 *   <li>{@link #WATCH}：文件系统 WatchService 监听 {@code *.properties} 变更；</li>
 *   <li>{@link #SUBSCRIBE}：Redis Pub/Sub 等外部变更通知；</li>
 *   <li>{@link #MANUAL}：仅手动触发（{@code ResourceReloader#reload}），用于配置中心回调等。</li>
 * </ul>
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public enum ReloadStrategy {

    /**
     * 定时轮询（数据库版本号变更检测）。
     */
    POLLING,

    /**
     * 文件系统监听（本地 *.properties 文件变更）。
     */
    WATCH,

    /**
     * 订阅通知（Redis Pub/Sub 等）。
     */
    SUBSCRIBE,

    /**
     * 手动触发。
     */
    MANUAL
}
