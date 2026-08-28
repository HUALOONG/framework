package cn.jowen.framework.data.core.datapermission;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 数据权限上下文：以线程变量承载当前生效的数据范围与作用表。
 *
 * <p>业务方在执行查询前调用 {@link #set(DataScope, String)} 声明范围，
 * 持久层（如 MyBatis Flex 扩展）读取该上下文并注入对应的数据过滤条件，
 * 方法结束后务必调用 {@link #clear()} 清理，避免线程复用导致串扰。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class DataPermissionContext {

    /** SCOPE 常量。 */
    private static final ThreadLocal<DataScope> SCOPE = new ThreadLocal<>();
    /** TABLE 常量。 */
    private static final ThreadLocal<String> TABLE = new ThreadLocal<>();

    private DataPermissionContext() {
    }

    /**
     * 设置当前数据范围。
     *
     * @param scope 数据范围，不可为 {@code null}
     * @param table 作用表名，为空表示作用于全部表
     */
    public static void set(DataScope scope, @Nullable String table) {
        SCOPE.set(scope);
        TABLE.set(table == null ? "" : table);
    }

    /** @return 当前数据范围，未设置时返回 {@link DataScope#ALL} */
    public static DataScope currentScope() {
        DataScope scope = SCOPE.get();
        return scope == null ? DataScope.ALL : scope;
    }

    /** @return 当前作用表名，未设置时返回空串 */
    public static String currentTable() {
        String table = TABLE.get();
        return table == null ? "" : table;
    }

    /** @return 是否已显式设置数据范围 */
    public static boolean active() {
        return SCOPE.get() != null;
    }

    /** 清理上下文 */
    public static void clear() {
        SCOPE.remove();
        TABLE.remove();
    }
}
