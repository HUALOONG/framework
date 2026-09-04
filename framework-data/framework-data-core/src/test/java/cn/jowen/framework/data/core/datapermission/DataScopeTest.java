package cn.jowen.framework.data.core.datapermission;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link DataScope} 测试：常量集合、名称反查与非法名称的失败分支。
 */
class DataScopeTest {

    @Test
    void values_containsAllScopes() {
        assertThat(DataScope.values()).containsExactly(
                DataScope.ALL,
                DataScope.SELF,
                DataScope.DEPARTMENT,
                DataScope.DEPARTMENT_AND_CHILD,
                DataScope.CUSTOM
        );
    }

    @Test
    void valueOf_resolvesEachConstant() {
        assertThat(DataScope.valueOf("ALL")).isSameAs(DataScope.ALL);
        assertThat(DataScope.valueOf("SELF")).isSameAs(DataScope.SELF);
        assertThat(DataScope.valueOf("DEPARTMENT")).isSameAs(DataScope.DEPARTMENT);
        assertThat(DataScope.valueOf("DEPARTMENT_AND_CHILD")).isSameAs(DataScope.DEPARTMENT_AND_CHILD);
        assertThat(DataScope.valueOf("CUSTOM")).isSameAs(DataScope.CUSTOM);
    }

    @Test
    void valueOf_unknownName_throws() {
        assertThatThrownBy(() -> DataScope.valueOf("NOT_EXIST"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ordinal_isStableForAll() {
        assertThat(DataScope.ALL.ordinal()).isZero();
        assertThat(DataScope.CUSTOM.ordinal()).isEqualTo(DataScope.values().length - 1);
    }
}
