package cn.jowen.framework.data.mybatis.query;

import cn.jowen.framework.data.core.datapermission.DataPermissionContext;
import cn.jowen.framework.data.core.datapermission.DataScope;
import cn.jowen.framework.data.core.query.Condition;
import cn.jowen.framework.data.core.query.Operator;
import cn.jowen.framework.data.core.query.QueryWrapper;
import cn.jowen.framework.data.mybatis.extension.FlexDataPermissionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 补充覆盖 {@link FlexQueryWrapperTranslator} 现有测试未触达的分支：
 * <ul>
 *   <li>{@code toLimitClause} 中 {@code limit <= 0} 时的 {@code LIMIT -1} 分支；</li>
 *   <li>{@code toSqlFragments(wrapper, dataPermission)} 中非空数据权限处理器注入条件路径。</li>
 * </ul>
 *
 * @author software-engineer-2
 */
class FlexQueryWrapperTranslatorCoverageTest {

    @AfterEach
    void cleanup() {
        DataPermissionContext.clear();
    }

    @Test
    void toLimitClause_zeroOrNegativeLimit_usesNegativeOne() {
        assertThat(FlexQueryWrapperTranslator.toLimitClause(0, null)).isEqualTo(" LIMIT -1");
        assertThat(FlexQueryWrapperTranslator.toLimitClause(-1, null)).isEqualTo(" LIMIT -1");
    }

    @Test
    void toSqlFragments_withDataPermission_appendsToExistingWhere() {
        QueryWrapper<?> wrapper = new QueryWrapper<>();
        wrapper.addCondition(new Condition(Operator.EQ, "status", 1));

        DataPermissionContext.set(DataScope.DEPARTMENT, "t_user");
        FlexDataPermissionHandler handler = new FlexDataPermissionHandler((scope, table) -> "dept_id = 5");
        try {
            String base = FlexQueryWrapperTranslator.toSqlFragments(wrapper);
            String sql = handler.enhanceWhere(base);
            assertThat(sql).contains("WHERE status = 1");
            assertThat(sql).contains("AND dept_id = 5");
        } finally {
            DataPermissionContext.clear();
        }
    }

    @Test
    void toSqlFragments_withDataPermission_noExistingWhere_prependsWhere() {
        QueryWrapper<?> wrapper = new QueryWrapper<>();

        DataPermissionContext.set(DataScope.DEPARTMENT, "t_user");
        FlexDataPermissionHandler handler = new FlexDataPermissionHandler((scope, table) -> "dept_id = 5");
        try {
            String base = FlexQueryWrapperTranslator.toSqlFragments(wrapper);
            String sql = handler.enhanceWhere(base);
            assertThat(sql).isEqualTo("WHERE dept_id = 5");
        } finally {
            DataPermissionContext.clear();
        }
    }
}
