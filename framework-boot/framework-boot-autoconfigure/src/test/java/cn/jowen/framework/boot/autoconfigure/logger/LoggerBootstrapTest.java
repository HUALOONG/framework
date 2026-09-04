package cn.jowen.framework.boot.autoconfigure.logger;

import cn.jowen.framework.logger.config.LoggerProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class LoggerBootstrapTest {

    @Test
    void disabled_doesNotWarmUp() {
        LoggerProperties props = new LoggerProperties();
        props.setEnabled(false);
        LoggerBootstrap bootstrap = new LoggerBootstrap(props);
        // 禁用时直接返回，不触发底层适配器解析
        assertThatCode(bootstrap::afterPropertiesSet).doesNotThrowAnyException();
    }
}
