package cn.jowen.framework.boot.autoconfigure.logger;

import cn.jowen.framework.logger.adapter.LoggerAdapter;
import cn.jowen.framework.logger.config.LoggerProperties;
import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * {@link LoggerBootstrap} 开启态预热测试。
 *
 * <p>既有 {@code LoggerBootstrapTest} 只覆盖「开关关闭即短路返回」的分支，
 * {@code afterPropertiesSet} 的适配器预热主体（含默认适配器解析与就绪日志）未触达。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class LoggerBootstrapGapTest {

    @Test
    void afterPropertiesSet_whenEnabled_warmsUpAdapterResolution() {
        LoggerProperties properties = new LoggerProperties();
        properties.setEnabled(true);

        assertThatCode(new LoggerBootstrap(properties)::afterPropertiesSet)
                .doesNotThrowAnyException();

        // 预热后门面与底层适配器均可解析，业务首次取 logger 不再触发 SPI 查找失败
        LoggerAdapter adapter = LoggerFactory.resolveAdapter();
        assertThat(adapter).isNotNull();
        assertThat(LoggerFactory.getLogger(LoggerBootstrap.class))
                .isInstanceOf(Logger.class);
    }
}
