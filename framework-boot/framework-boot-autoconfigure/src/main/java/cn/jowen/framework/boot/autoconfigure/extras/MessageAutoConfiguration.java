package cn.jowen.framework.boot.autoconfigure.extras;

import cn.jowen.framework.extras.message.core.DefaultMessageChannel;
import cn.jowen.framework.extras.message.core.DefaultMessageService;
import cn.jowen.framework.extras.message.core.MessageChannel;
import cn.jowen.framework.extras.message.core.MessageSender;
import cn.jowen.framework.extras.message.core.MessageService;
import cn.jowen.framework.extras.message.template.SimpleTemplateEngine;
import cn.jowen.framework.extras.message.template.TemplateEngine;
import org.jspecify.annotations.NullMarked;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * 消息模块自动装配。
 *
 * <p>按 {@link MessageSender} 实例集合构建 {@link MessageChannel} 路由，
 * 并提供 {@link MessageService} 门面。用户注入自己的 {@link MessageSender}
 * 实现即可接入对应渠道。
 *
 * <p>行为由 {@link BootMessageProperties}（{@code framework.extras.message.*}）驱动：
 * {@code async} 决定异步执行器，{@code maxRetries} 决定失败重试次数。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@AutoConfiguration
@ConditionalOnProperty(prefix = "framework.extras", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(BootMessageProperties.class)
public class MessageAutoConfiguration {

    /** props 不可变字段。 */
    private final BootMessageProperties props;

    /**
     * 构造实例。
     * @param props 参数 props
     */
    public MessageAutoConfiguration(BootMessageProperties props) {
        this.props = props;
    }

    /**
     * 执行message channel操作。
     * @return 结果
     */
    @Bean
    @ConditionalOnMissingBean
    public MessageChannel messageChannel(List<MessageSender> senders) {
        return new DefaultMessageChannel(senders);
    }

    /**
     * 执行template engine操作。
     * @return 结果
     */
    @Bean
    @ConditionalOnMissingBean
    public TemplateEngine templateEngine() {
        return new SimpleTemplateEngine();
    }

    /**
     * 执行message executor操作。
     * @return 结果
     */
    @Bean
    @ConditionalOnMissingBean
    public Executor messageExecutor() {
        return ForkJoinPool.commonPool();
    }

    /**
     * 执行message service操作。
     * @param channel 参数 channel
     * @param templateEngine 参数 templateEngine
     * @param executor 参数 executor
     * @return 结果
     */
    @Bean
    @ConditionalOnMissingBean
    public MessageService messageService(MessageChannel channel, TemplateEngine templateEngine,
                                         Executor executor) {
        return new DefaultMessageService(channel, templateEngine, executor,
                props.isAsync(), props.getMaxRetries());
    }
}
