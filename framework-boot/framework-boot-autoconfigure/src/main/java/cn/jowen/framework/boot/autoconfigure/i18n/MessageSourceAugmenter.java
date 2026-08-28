package cn.jowen.framework.boot.autoconfigure.i18n;

import cn.jowen.framework.i18n.source.CompositeMessageSource;
import org.jspecify.annotations.NullMarked;

/**
 * 消息源增补器：向 {@link CompositeMessageSource} 追加额外的消息源实现。
 *
 * <p><b>存在意义</b>：Redis 消息源依赖 optional 的 Redisson。若直接在
 * {@code I18nAutoConfiguration#messageSource} 的方法签名上声明
 * {@code ObjectProvider<RedissonClient>}，Spring 在解析该方法的泛型参数时会触发
 * {@code RedissonClient} 的类加载，导致<b>未引入 Redisson 的应用启动即失败</b>
 * （{@code ClassNotFoundException}）——即使该参数从未被使用。
 *
 * <p>通过本接口将「是否需要 Redis 源」的判断下沉到
 * {@code @ConditionalOnClass} 保护的内部配置类中，主装配方法签名内
 * 不再出现任何 optional SDK 类型，从而保证按需引入真正生效。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@FunctionalInterface
public interface MessageSourceAugmenter {

    /**
     * 尝试向组合消息源追加一个子源。
     *
     * @param composite  组合消息源，不可为 {@code null}
     * @param properties i18n 配置，不可为 {@code null}
     * @return 是否成功追加了子源；返回 {@code false} 时调用方会走兜底逻辑
     */
    boolean augment(CompositeMessageSource composite, BootI18nProperties properties);
}
