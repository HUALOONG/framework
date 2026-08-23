package cn.jowen.framework.extras.datapermission;

import org.jspecify.annotations.NullMarked;

/**
 * 数据权限规则接口：将用户与表信息转换为参数化 SQL 片段。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public interface DataPermissionRule {

    /**
     * 计算数据权限表达式。
     *
     * @param user  当前用户（不可为 null）
     * @param table 表信息（不可为 null）
     * @return 参数化 SQL 片段与参数列表
     */
    DataPermissionExpression getExpression(UserInfo user, TableInfo table);
}
