package cn.jowen.framework.demo.config;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import cn.jowen.framework.logger.mask.LogMaskLayout;

/**
 * 脱敏日志布局：在 Logback 输出前对整行日志施加脱敏。
 *
 * <p>框架提供了 {@link LogMaskLayout} 装饰器，但它是一个普通类而非 Logback 的
 * {@code Layout} 实现——框架<b>刻意不接管应用的日志配置</b>，
 * 需要接入哪种 Appender/Encoder 由应用决定。本类即是接入示例。
 *
 * <p>配合 {@code logback-spring.xml} 使用：
 * <pre>{@code
 * <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
 *     <layout class="cn.jowen.framework.demo.config.MaskingPatternLayout">
 *         <pattern>%d{...} %-5level ...</pattern>
 *     </layout>
 * </encoder>
 * }</pre>
 *
 * <p><b>扩展提示</b>：若使用 Log4j2，改为实现其 {@code Layout} 接口并做同样的包装即可。
 *
 * @author demo
  * @since 0.0.1
 * @version 0.0.1
 */
public class MaskingPatternLayout extends PatternLayout {

    /** masker 不可变字段。 */
    private final LogMaskLayout masker = new LogMaskLayout();

    /**
     * 执行do layout操作。
     * @param event 参数 event
     * @return 结果
     */
    @Override
    public String doLayout(ILoggingEvent event) {
        String formatted = super.doLayout(event);
        return masker.decorate(formatted);
    }
}
