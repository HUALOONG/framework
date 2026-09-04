package cn.jowen.framework.extras.web.desensitize;

import cn.jowen.framework.core.desensitize.DesensitizeField;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link DesensitizeAspect} 环绕织入测试：
 * 覆盖切面构造、返回值脱敏分支、空返回值分支以及目标方法抛异常原样上抛。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class DesensitizeAspectTest {

    /** 测试 VO（含字段级脱敏注解）。 */
    public static class Vo {
        /** 手机号。 */
        @DesensitizeField(strategy = "PHONE")
        public String phone;

        Vo(String phone) {
            this.phone = phone;
        }
    }

    @Test
    void around_masksReturnValue() throws Throwable {
        DesensitizeAspect aspect = new DesensitizeAspect();
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        Desensitized desensitized = mock(Desensitized.class);
        Vo vo = new Vo("13812345678");
        when(pjp.proceed()).thenReturn(vo);

        Object result = aspect.around(pjp, desensitized);

        // 返回值被原地脱敏，引用不变
        assertThat(result).isSameAs(vo);
        assertThat(vo.phone).isEqualTo("138****5678");
    }

    @Test
    void around_nullResult_returnsNullWithoutDesensitization() throws Throwable {
        DesensitizeAspect aspect = new DesensitizeAspect();
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        Desensitized desensitized = mock(Desensitized.class);
        when(pjp.proceed()).thenReturn(null);

        // null 返回值原样返回，desensitize 分支不生效
        assertThat(aspect.around(pjp, desensitized)).isNull();
    }

    @Test
    void around_proceedThrows_propagatesOriginal() throws Throwable {
        DesensitizeAspect aspect = new DesensitizeAspect();
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        Desensitized desensitized = mock(Desensitized.class);
        when(pjp.proceed()).thenThrow(new IllegalStateException("boom"));

        // 目标方法抛出的异常原样上抛，不被包裹
        assertThatThrownBy(() -> aspect.around(pjp, desensitized))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("boom");
    }
}
