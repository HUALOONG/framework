package cn.jowen.framework.boot.autoconfigure.extras;

import cn.jowen.framework.extras.web.desensitize.DesensitizeAspect;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link WebExtrasAutoConfiguration} 默认关闭能力的方法体覆盖测试。
 *
 * <p>既有 {@code WebExtrasAutoConfigurationTest} 通过 {@code ApplicationContextRunner} 验证条件装配，
 * 而 {@code desensitizeAspect()} 因默认关闭（{@code framework.extras.web.desensitize.enabled=false}）
 * 在容器中不会被创建，方法体未触达。本类直接实例化装配类调用该方法。
 *
 * <p><b>为何不覆盖 {@code ExcelConfiguration}</b>：该类由 {@code @ConditionalOnClass(name =
 * "com.alibaba.excel.EasyExcel")} 保护，EasyExcel 为 optional 依赖且不在本模块测试 classpath，
 * 直接调用 {@code excelExporter()} / {@code excelImporter()} 会抛 {@code NoClassDefFoundError}，
 * 属结构性不可覆盖。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class WebExtrasAutoConfigurationGapTest {

    @Test
    void desensitizeAspect_createsAspect() {
        assertThat(new WebExtrasAutoConfiguration(new BootWebExtrasProperties()).desensitizeAspect())
                .isInstanceOf(DesensitizeAspect.class);
    }
}
