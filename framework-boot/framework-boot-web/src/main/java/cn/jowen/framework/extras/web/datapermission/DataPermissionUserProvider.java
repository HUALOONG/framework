package cn.jowen.framework.extras.web.datapermission;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 数据权限用户上下文提供者：解析当前登录用户的标识与所属部门。
 *
 * <p>框架不耦合任何具体安全框架（Spring Security / Sa-Token / 自研 Session 等），
 * 由业务方注入实现。未注入时使用 {@link #NONE}，此时
 * {@link DataPermissionRule.SqlDefault} 对受限范围会<b>显式抛异常</b>而非静默放行，
 * 避免"数据权限形同虚设"却难以察觉。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface DataPermissionUserProvider {

    /** @return 当前用户 ID，未知返回 {@code null} */
    @Nullable String currentUserId();

    /** @return 当前用户所属部门 ID，未知返回 {@code null} */
    @Nullable String currentDeptId();

    /**
     * @return 当前用户可见的部门 ID 列表（含子部门），未知返回空列表。
     *     用于 {@link cn.jowen.framework.extras.properties.DataScope#DEPT_AND_CHILD}
     */
    List<String> currentDeptAndChildIds();

    /** 默认实现：不解析用户上下文 */
    DataPermissionUserProvider NONE = new DataPermissionUserProvider() {
        @Override
        public @Nullable String currentUserId() {
            return null;
        }

        @Override
        public @Nullable String currentDeptId() {
            return null;
        }

        @Override
        public List<String> currentDeptAndChildIds() {
            return List.of();
        }
    };
}
