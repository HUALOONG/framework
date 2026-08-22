package cn.jowen.framework.logger;

import cn.jowen.framework.core.desensitize.Desensitizer;
import cn.jowen.framework.logger.config.LoggerProperties;
import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import cn.jowen.framework.logger.mask.LogMasker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 冒烟测试：验证 M1 出口标准——脱敏生效、日志门面可用。
 */
class MaskAndFacadeTest {

    @Test
    void desensitizePhoneAndIdCard() {
        String text = "用户 13812341234 身份证 110101199001011234 登录";
        String masked = Desensitizer.getInstance().mask(text);
        assertNotNull(masked);
        assertTrue(masked.contains("138****1234"), "手机号应脱敏");
        assertTrue(masked.contains("110********1234"), "身份证应脱敏");
    }

    @Test
    void logMaskerRespectsSwitch() {
        LoggerProperties on = new LoggerProperties();
        on.setDesensitizeEnabled(true);
        assertEquals("138****1234", new LogMasker(on).maskMessage("13812341234"));

        LoggerProperties off = new LoggerProperties();
        off.setDesensitizeEnabled(false);
        assertEquals("13812341234", new LogMasker(off).maskMessage("13812341234"));
    }

    @Test
    void loggerFactoryResolvesAdapter() {
        Logger log = LoggerFactory.getLogger(MaskAndFacadeTest.class);
        assertNotNull(log);
        log.info("脱敏测试 手机号 {}", "13812341234");
    }
}
