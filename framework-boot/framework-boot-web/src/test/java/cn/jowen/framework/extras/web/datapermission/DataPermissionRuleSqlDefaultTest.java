package cn.jowen.framework.extras.web.datapermission;

import cn.jowen.framework.extras.common.exception.DataPermissionException;
import cn.jowen.framework.extras.properties.DataScope;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link DataPermissionRule.SqlDefault} 单元测试。
 *
 * <p>覆盖：各数据范围的条件生成、自定义列名、单引号转义（注入防护）、
 * 以及"取不到用户就显式失败"的安全约定。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
class DataPermissionRuleSqlDefaultTest {

    private static DataPermissionUserProvider user(String userId, String deptId, List<String> deptIds) {
        return new DataPermissionUserProvider() {
            @Override
            public String currentUserId() {
                return userId;
            }

            @Override
            public String currentDeptId() {
                return deptId;
            }

            @Override
            public List<String> currentDeptAndChildIds() {
                return deptIds;
            }
        };
    }

    private static final DataPermissionUserProvider PROVIDER =
            user("u1", "d1", List.of("d1", "d2", "d3"));

    private static final DataPermissionRule RULE =
            new DataPermissionRule.SqlDefault("dept_id", "create_by", PROVIDER);

    @Test
    void allScopeReturnsNull() {
        assertThat(RULE.condition(DataScope.ALL, "t_order")).isNull();
    }

    @Test
    void customScopeReturnsNull() {
        assertThat(RULE.condition(DataScope.CUSTOM, "t_order")).isNull();
    }

    @Test
    void selfScopeGeneratesUserEquality() {
        assertThat(RULE.condition(DataScope.SELF, "t_order")).isEqualTo("create_by = 'u1'");
    }

    @Test
    void deptScopeGeneratesDeptEquality() {
        assertThat(RULE.condition(DataScope.DEPT, "t_order")).isEqualTo("dept_id = 'd1'");
    }

    @Test
    void deptAndChildScopeGeneratesInClause() {
        assertThat(RULE.condition(DataScope.DEPT_AND_CHILD, "t_order"))
                .isEqualTo("dept_id IN ('d1','d2','d3')");
    }

    @Test
    void deptAndChildWithSingleDeptGeneratesSingleElementIn() {
        DataPermissionRule rule = new DataPermissionRule.SqlDefault(
                "dept_id", "create_by", user("u1", "d9", List.of("d9")));

        assertThat(rule.condition(DataScope.DEPT_AND_CHILD, "t")).isEqualTo("dept_id IN ('d9')");
    }

    @Test
    void customColumnNamesAreRespected() {
        DataPermissionRule rule =
                new DataPermissionRule.SqlDefault("org_id", "owner", PROVIDER);

        assertThat(rule.condition(DataScope.SELF, "t")).isEqualTo("owner = 'u1'");
        assertThat(rule.condition(DataScope.DEPT, "t")).isEqualTo("org_id = 'd1'");
    }

    @Test
    void singleQuoteInValueIsEscaped() {
        DataPermissionRule rule = new DataPermissionRule.SqlDefault(
                "dept_id", "create_by", user("o'brien", "d'1", List.of("d'1")));

        assertThat(rule.condition(DataScope.SELF, "t")).isEqualTo("create_by = 'o''brien'");
        assertThat(rule.condition(DataScope.DEPT, "t")).isEqualTo("dept_id = 'd''1'");
        assertThat(rule.condition(DataScope.DEPT_AND_CHILD, "t"))
                .isEqualTo("dept_id IN ('d''1')");
    }

    @Test
    void missingUserContextFailsLoudlyForSelf() {
        DataPermissionRule rule =
                new DataPermissionRule.SqlDefault("dept_id", "create_by", DataPermissionUserProvider.NONE);

        assertThatThrownBy(() -> rule.condition(DataScope.SELF, "t"))
                .isInstanceOf(DataPermissionException.class)
                .hasMessageContaining("当前用户 ID");
    }

    @Test
    void missingUserContextFailsLoudlyForDept() {
        DataPermissionRule rule =
                new DataPermissionRule.SqlDefault("dept_id", "create_by", DataPermissionUserProvider.NONE);

        assertThatThrownBy(() -> rule.condition(DataScope.DEPT, "t"))
                .isInstanceOf(DataPermissionException.class)
                .hasMessageContaining("当前部门 ID");
    }

    @Test
    void missingDeptListFailsLoudlyForDeptAndChild() {
        DataPermissionRule rule =
                new DataPermissionRule.SqlDefault("dept_id", "create_by", DataPermissionUserProvider.NONE);

        assertThatThrownBy(() -> rule.condition(DataScope.DEPT_AND_CHILD, "t"))
                .isInstanceOf(DataPermissionException.class)
                .hasMessageContaining("可见部门列表");
    }

    @Test
    void blankUserIdIsTreatedAsMissing() {
        DataPermissionRule rule =
                new DataPermissionRule.SqlDefault("dept_id", "create_by", user("  ", "d1", List.of("d1")));

        assertThatThrownBy(() -> rule.condition(DataScope.SELF, "t"))
                .isInstanceOf(DataPermissionException.class);
    }

    @Test
    void nullProviderIsNotAllowedForRestrictedScopes() {
        DataPermissionRule rule = new DataPermissionRule.SqlDefault(
                "dept_id", "create_by", user(null, null, List.of()));

        assertThatThrownBy(() -> rule.condition(DataScope.SELF, "t"))
                .isInstanceOf(DataPermissionException.class);
    }

    @Test
    void noneProviderReturnsEmptyDeptListAndNullIds() {
        assertThat(DataPermissionUserProvider.NONE.currentUserId()).isNull();
        assertThat(DataPermissionUserProvider.NONE.currentDeptId()).isNull();
        assertThat(DataPermissionUserProvider.NONE.currentDeptAndChildIds()).isEmpty();
    }
}
