package cn.jowen.framework.data.core.datapermission;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DataPermissionRule} 测试：默认实现 {@link DataPermissionRule.Default} 的过滤条件生成，
 * 以及 SPI 作为函数式接口可被业务覆盖的能力。
 */
class DataPermissionRuleTest {

    private final DataPermissionRule rule = new DataPermissionRule.Default();

    @Test
    void allScope_returnsNull_noFiltering() {
        assertThat(rule.condition(DataScope.ALL, "sys_user")).isNull();
    }

    @Test
    void allScope_ignoresTableName() {
        assertThat(rule.condition(DataScope.ALL, "")).isNull();
    }

    @Test
    void nonAllScope_returnsEmptyCondition() {
        assertThat(rule.condition(DataScope.SELF, "sys_user")).isEmpty();
        assertThat(rule.condition(DataScope.DEPARTMENT, "sys_user")).isEmpty();
        assertThat(rule.condition(DataScope.DEPARTMENT_AND_CHILD, "sys_user")).isEmpty();
        assertThat(rule.condition(DataScope.CUSTOM, "sys_user")).isEmpty();
    }

    @Test
    void customRule_canOverrideDefault() {
        DataPermissionRule custom = (scope, table) -> "tenant_id = 1 AND " + table + ".scope = '" + scope + "'";

        assertThat(custom.condition(DataScope.SELF, "sys_user"))
                .isEqualTo("tenant_id = 1 AND sys_user.scope = 'SELF'");
    }
}
