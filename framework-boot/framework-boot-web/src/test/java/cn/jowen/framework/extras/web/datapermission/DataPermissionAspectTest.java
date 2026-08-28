package cn.jowen.framework.extras.web.datapermission;

import cn.jowen.framework.extras.properties.DataScope;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataPermissionAspectTest {

    @Mock
    private DataPermissionRule rule;
    @Mock
    private ProceedingJoinPoint pjp;
    @Mock
    private DataPermission dataPermission;

    @Test
    void around_setsContextDuringExecutionAndClearsAfter() throws Throwable {
        when(dataPermission.scope()).thenReturn(DataScope.SELF);
        when(dataPermission.table()).thenReturn("orders");
        when(pjp.proceed()).thenAnswer(invocation -> {
            assertThat(DataPermissionContext.isActive()).isTrue();
            assertThat(DataPermissionContext.currentScope()).isEqualTo(DataScope.SELF);
            assertThat(DataPermissionContext.currentTable()).isEqualTo("orders");
            return "ok";
        });

        DataPermissionAspect aspect = new DataPermissionAspect(rule);
        Object result = aspect.around(pjp, dataPermission);

        assertThat(result).isEqualTo("ok");
        assertThat(DataPermissionContext.isActive()).isFalse();
        assertThat(DataPermissionContext.currentTable()).isEmpty();
    }

    @Test
    void around_clearsContextWhenProceedThrows() throws Throwable {
        when(dataPermission.scope()).thenReturn(DataScope.DEPT);
        when(dataPermission.table()).thenReturn("orders");
        when(pjp.proceed()).thenThrow(new IllegalStateException("boom"));

        DataPermissionAspect aspect = new DataPermissionAspect(rule);

        assertThatThrownBy(() -> aspect.around(pjp, dataPermission))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
        assertThat(DataPermissionContext.isActive()).isFalse();
    }

    @Test
    void currentCondition_withActiveContext_usesContextScope() {
        DataPermissionContext.set(DataScope.SELF, "orders");
        when(rule.condition(DataScope.SELF, "orders")).thenReturn("create_by = 'u1'");

        DataPermissionAspect aspect = new DataPermissionAspect(rule);

        assertThat(aspect.currentCondition()).isEqualTo("create_by = 'u1'");
        DataPermissionContext.clear();
    }

    @Test
    void currentCondition_withoutActiveContext_usesDefaultScope() {
        when(rule.condition(DataScope.DEPT, "")).thenReturn("dept_id = 'd1'");

        DataPermissionAspect aspect = new DataPermissionAspect(rule, DataScope.DEPT);

        assertThat(aspect.currentCondition()).isEqualTo("dept_id = 'd1'");
    }

    @Test
    void currentCondition_returnsNullWhenRuleReturnsNull() {
        when(rule.condition(DataScope.ALL, "")).thenReturn(null);

        DataPermissionAspect aspect = new DataPermissionAspect(rule);

        assertThat(aspect.currentCondition()).isNull();
    }
}
