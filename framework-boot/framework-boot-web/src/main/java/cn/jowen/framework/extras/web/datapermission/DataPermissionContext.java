package cn.jowen.framework.extras.web.datapermission;

import cn.jowen.framework.extras.properties.DataScope;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 数据权限上下文：以线程变量承载当前生效的数据范围。
 *
 * <p>由 {@link DataPermissionAspect} 在方法进入时写入、退出时清理；
 * 持久层拦截器读取该上下文生成对应的数据过滤条件。
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
     * @param scope 数据范围
     * @param table 作用表名（可为空）
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

    /**
     * @return 当前线程是否存在活跃的 {@link DataPermission} 上下文
     *
     * <p>用于区分两种 {@link DataScope#ALL}：
     * <ul>
     *   <li>{@code true} —— 方法显式标注了 {@code @DataPermission(scope = ALL)}，意为"本方法放行全部数据"；</li>
     *   <li>{@code false} —— 方法根本未标注注解（切面未介入），此时应回落到配置的默认数据范围。</li>
     * </ul>
     *
     * 两者经 {@link #currentScope()} 读取均为 {@code ALL} 而无法区分，
     * 因此必须借助本方法，否则配置的默认范围会误覆盖显式放行的方法。
     */
    public static boolean isActive() {
        return SCOPE.get() != null;
    }

    /** @return 当前作用表名，未设置返回空串 */
    public static String currentTable() {
        String table = TABLE.get();
        return table == null ? "" : table;
    }

    /** 清理上下文 */
    public static void clear() {
        SCOPE.remove();
        TABLE.remove();
    }
}
