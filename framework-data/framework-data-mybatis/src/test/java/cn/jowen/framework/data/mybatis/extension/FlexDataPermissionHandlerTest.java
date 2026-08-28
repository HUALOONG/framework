package cn.jowen.framework.data.mybatis.extension;

import cn.jowen.framework.data.core.datapermission.DataPermissionContext;
import cn.jowen.framework.data.core.datapermission.DataPermissionRule;
import cn.jowen.framework.data.core.datapermission.DataScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 覆盖 {@link FlexDataPermissionHandler} 的全部分支（条件生成、WHERE 合并、范围判断）。
 * 数据权限上下文基于线程变量，测试后统一清理。
 *
 * @author software-engineer-2
 */
class FlexDataPermissionHandlerTest {

    @AfterEach
    void cleanup() {
        DataPermissionContext.clear();
    }

    @Test
    void metadata_returnsExtensionNameAndOrder() {
        FlexDataPermissionHandler handler = new FlexDataPermissionHandler();
        assertThat(handler.name()).isEqualTo("dataPermission");
        assertThat(handler.order()).isEqualTo(90);
    }

    @Test
    void requiresFilter_onlyAllScopeIsFalse() {
        FlexDataPermissionHandler handler = new FlexDataPermissionHandler();
        assertThat(handler.requiresFilter(DataScope.ALL)).isFalse();
        assertThat(handler.requiresFilter(DataScope.SELF)).isTrue();
        assertThat(handler.requiresFilter(DataScope.DEPARTMENT)).isTrue();
        assertThat(handler.requiresFilter(DataScope.DEPARTMENT_AND_CHILD)).isTrue();
        assertThat(handler.requiresFilter(DataScope.CUSTOM)).isTrue();
    }

    @Test
    void condition_inactiveContext_returnsNull() {
        assertThat(DataPermissionContext.active()).isFalse();
        FlexDataPermissionHandler handler = new FlexDataPermissionHandler();
        assertThat(handler.condition()).isNull();
    }

    @Test
    void condition_activeAllScope_defaultRule_returnsNull() {
        DataPermissionContext.set(DataScope.ALL, "t_user");
        FlexDataPermissionHandler handler = new FlexDataPermissionHandler();
        assertThat(handler.condition()).isNull();
    }

    @Test
    void condition_activeScope_customRule_returnsFragment() {
        DataPermissionContext.set(DataScope.DEPARTMENT, "t_user");
        DataPermissionRule rule = (scope, table) -> "dept_id = 5";
        FlexDataPermissionHandler handler = new FlexDataPermissionHandler(rule);
        assertThat(handler.condition()).isEqualTo("dept_id = 5");
    }

    @Test
    void enhanceWhere_inactiveContext_returnsOriginal() {
        FlexDataPermissionHandler handler = new FlexDataPermissionHandler();
        assertThat(handler.enhanceWhere(null)).isEmpty();
        assertThat(handler.enhanceWhere("WHERE a = 1")).isEqualTo("WHERE a = 1");
    }

    @Test
    void enhanceWhere_activeContextBlankCondition_returnsOriginal() {
        DataPermissionContext.set(DataScope.DEPARTMENT, "t_user");
        // 默认规则对非空范围返回空串（视为不过滤）
        FlexDataPermissionHandler handler = new FlexDataPermissionHandler();
        assertThat(handler.enhanceWhere("WHERE a = 1")).isEqualTo("WHERE a = 1");
    }

    @Test
    void enhanceWhere_activeContextWithCondition_noExistingWhere() {
        DataPermissionContext.set(DataScope.DEPARTMENT, "t_user");
        FlexDataPermissionHandler handler = new FlexDataPermissionHandler((scope, table) -> "dept_id = 5");
        assertThat(handler.enhanceWhere(null)).isEqualTo("WHERE dept_id = 5");
        assertThat(handler.enhanceWhere("")).isEqualTo("WHERE dept_id = 5");
    }

    @Test
    void enhanceWhere_activeContextWithCondition_existingWhere() {
        DataPermissionContext.set(DataScope.DEPARTMENT, "t_user");
        FlexDataPermissionHandler handler = new FlexDataPermissionHandler((scope, table) -> "dept_id = 5");
        assertThat(handler.enhanceWhere("WHERE a = 1")).isEqualTo("WHERE a = 1 AND dept_id = 5");
    }
}
