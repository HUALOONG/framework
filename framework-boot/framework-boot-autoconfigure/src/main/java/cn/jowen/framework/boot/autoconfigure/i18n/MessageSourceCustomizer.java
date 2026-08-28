package cn.jowen.framework.boot.autoconfigure.i18n;

import cn.jowen.framework.i18n.api.MessageSource;
import org.jspecify.annotations.NullMarked;

/**
 * 消息源定制器：装配完成后由容器调用，允许追加子源、调整格式化策略等，
 * 避免业务方直接持有框架 Bean 引用。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@FunctionalInterface
public interface MessageSourceCustomizer {

    /**
     * 定制消息源。
     *
     * @param messageSource 已装配的消息源（通常为 {@code CompositeMessageSource}），不可为 {@code null}
     */
    void customize(MessageSource messageSource);
}
