package cn.jowen.framework.i18n.source;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import java.util.Locale;

/**
 * Redis 消息源：以 {@code i18n:{basename}:{localeTag}} 哈希表存储 编码→文案，
 * 复用 {@link AbstractMessageSource} 的区域回退链与参数化格式化。
 *
 * <p>Redis 即数据源本身（无本地缓存），{@code reload()} 为空操作；跨节点变更可通过
 * {@link cn.jowen.framework.i18n.reload.RedisSubscriptionWatcher} 订阅通知。Redisson 为可选依赖。
 *
 * @author 王飞
 * @since 2026-08-27
 */
@NullMarked
public final class RedisMessageSource extends AbstractMessageSource {

    private static final String LOCALE_ROOT = "root";
    private static final String KEY_PREFIX = "i18n:";

    private final RedissonClient client;
    private final String basename;

    /**
     * 构造 Redis 消息源。
     *
     * @param client   Redisson 客户端，不可为 {@code null}
     * @param basename 消息键前缀（如 {@code messages}），不可为 {@code null}
     */
    public RedisMessageSource(RedissonClient client, String basename) {
        this.client = client;
        this.basename = basename;
    }

    @Override
    protected @Nullable String loadRaw(String code, Locale locale) {
        RMap<String, String> map = client.getMap(mapKey(basename, locale));
        return map.get(code);
    }

    /**
     * 计算区域哈希表键。
     *
     * @param basename 消息键前缀
     * @param locale   区域；语言为空时归入 root
     * @return Redis 哈希表键
     */
    static String mapKey(String basename, Locale locale) {
        String localePart = locale.getLanguage().isEmpty() ? LOCALE_ROOT : locale.toLanguageTag();
        return KEY_PREFIX + basename + ":" + localePart;
    }
}