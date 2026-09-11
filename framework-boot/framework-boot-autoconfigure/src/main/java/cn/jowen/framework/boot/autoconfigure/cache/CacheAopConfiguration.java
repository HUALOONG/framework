package cn.jowen.framework.boot.autoconfigure.cache;

import cn.jowen.framework.cache.annotation.EnableCaching;
import cn.jowen.framework.cache.condition.ConditionEvaluator;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * 缓存注解启用配置。当 classpath 存在 Spring AOP 时生效，注册 {@link SpringCacheAnnotationProcessor}
 * 切面并自动激活 {@link EnableCaching} 代理支持。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@Configuration
@ConditionalOnClass(name = {
        "org.aspectj.lang.annotation.Aspect",
        "cn.jowen.framework.cache.annotation.Cacheable",
        "org.springframework.expression.spel.standard.SpelExpressionParser"
})
public class CacheAopConfiguration {

    /**
     * 缓存注解 AOP 切面，拦截 @Cacheable/@CachePut/@CacheEvict。
     * 由 SpringCacheAnnotationProcessor 实现，此处确保 Bean 可用。
     */
    @Bean
    @ConditionalOnMissingBean(name = "springCacheAnnotationProcessor")
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SpringCacheAnnotationProcessor springCacheAnnotationProcessor() {
        return new SpringCacheAnnotationProcessor();
    }

    /**
     * 条件表达式解析器，用于求值 {@code condition} / {@code unless} 表达式。
     * 默认基于 Spring SpEL；业务方可替换为自定义实现。
     */
    @Bean
    @ConditionalOnMissingBean
    public ConditionEvaluator conditionEvaluator() {
        return new SpelConditionEvaluator();
    }
}
