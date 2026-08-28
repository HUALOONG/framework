package cn.jowen.framework.data.mybatis.extension;

import cn.jowen.framework.data.core.datapermission.DataPermissionContext;
import cn.jowen.framework.data.core.datapermission.DataPermissionRule;
import cn.jowen.framework.data.core.datapermission.DataScope;
import cn.jowen.framework.data.mybatis.extension.ExtensionRegistry.Extension;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 数据权限扩展：将 {@link DataPermissionContext} 声明的数据范围转换为 SQL 过滤条件，
 * 并追加到查询的 WHERE 子句中。
 *
 * <p>使用方式：
 * <pre>
 *   DataPermissionContext.set(DataScope.DEPARTMENT, "sys_user");
 *   try {
 *       String where = handler.enhanceWhere("WHERE status = 1"); // 追加部门过滤条件
 *   } finally {
 *       DataPermissionContext.clear();
 *   }
 * </pre>
 *
 * <p><b>安全提示</b>：{@link DataPermissionRule} 返回的是 SQL 片段，业务实现必须保证
 * 其中的值来自可信来源或已做参数化处理，禁止直接拼接用户输入，以免引入 SQL 注入。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class FlexDataPermissionHandler implements Extension {

    /** rule 不可变字段。 */
    private final DataPermissionRule rule;

    /**
     * 构造实例。
     */
    public FlexDataPermissionHandler() {
        this(new DataPermissionRule.Default());
    }

    /**
     * 构造实例。
     * @param rule 参数 rule
     */
    public FlexDataPermissionHandler(DataPermissionRule rule) {
        this.rule = rule;
    }

    /**
     * 执行name操作。
     * @return 结果
     */
    @Override
    public String name() {
        return "dataPermission";
    }

    /**
     * 执行order操作。
     * @return 结果
     */
    @Override
    public int order() {
        return 90;
    }

    /**
     * 生成当前上下文对应的数据权限条件片段（不含 WHERE 关键字）。
     *
     * @return 条件片段；无需过滤返回 {@code null} 或空串
     */
    public @Nullable String condition() {
        if (!DataPermissionContext.active()) {
            return null;
        }
        return rule.condition(DataPermissionContext.currentScope(), DataPermissionContext.currentTable());
    }

    /**
     * 将数据权限条件追加到已有 WHERE 子句。
     *
     * @param existingWhere 已有 WHERE 子句（可为空或 {@code null}）
     * @return 合并后的 WHERE 子句
     */
    public String enhanceWhere(@Nullable String existingWhere) {
        String cond = condition();
        if (cond == null || cond.isBlank()) {
            return existingWhere == null ? "" : existingWhere;
        }
        if (existingWhere == null || existingWhere.isBlank()) {
            return "WHERE " + cond;
        }
        return existingWhere + " AND " + cond;
    }

    /**
     * 判断指定范围是否需要过滤。
     *
     * @param scope 数据范围
     * @return 需要过滤返回 {@code true}
     */
    public boolean requiresFilter(DataScope scope) {
        return scope != DataScope.ALL;
    }
}
