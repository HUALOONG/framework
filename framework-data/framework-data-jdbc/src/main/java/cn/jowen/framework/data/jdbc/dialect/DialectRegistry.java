package cn.jowen.framework.data.jdbc.dialect;

import cn.jowen.framework.data.core.dialect.DatabaseDialect;
import cn.jowen.framework.data.core.dialect.DatabaseType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * 方言注册中心：维护 {@link DatabaseType} → {@link DatabaseDialect} 映射，并提供默认方言与按产品名探测。
 *
 * <p>构造时注册内置方言（H2 / MySQL / PostgreSQL / Oracle / SQLServer），默认方言为 H2（便于独立运行与测试）。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class DialectRegistry {

    private final Map<DatabaseType, DatabaseDialect> dialects = new EnumMap<>(DatabaseType.class);
    private @Nullable DatabaseDialect defaultDialect;

    public DialectRegistry() {
        registerDefaults();
    }

    private void registerDefaults() {
        H2Dialect h2 = new H2Dialect();
        MySQLDialect mysql = new MySQLDialect();
        PostgreSQLDialect postgre = new PostgreSQLDialect();
        OracleDialect oracle = new OracleDialect();
        SQLServerDialect sqlServer = new SQLServerDialect();
        register(h2);
        register(mysql);
        register(postgre);
        register(oracle);
        register(sqlServer);
        this.defaultDialect = h2;
    }

    /**
     * 注册方言；若尚未设置默认方言，则以首个注册方言为默认。
     *
     * @param dialect 方言，不可为 {@code null}
     */
    public void register(DatabaseDialect dialect) {
        dialects.put(dialect.type(), dialect);
        if (defaultDialect == null) {
            defaultDialect = dialect;
        }
    }

    /**
     * 按数据库类型取方言。
     *
     * @param type 数据库类型，不可为 {@code null}
     * @return 方言，未注册返回 {@code null}
     */
    public @Nullable DatabaseDialect get(DatabaseType type) {
        return dialects.get(type);
    }

    /**
     * 取默认方言。
     *
     * @return 默认方言，不可为 {@code null}
     * @throws IllegalStateException 未注册任何方言
     */
    public DatabaseDialect getDefault() {
        if (defaultDialect == null) {
            throw new IllegalStateException("未注册任何方言");
        }
        return defaultDialect;
    }

    /**
     * 设置默认方言实例。
     *
     * @param dialect 方言，不可为 {@code null}
     */
    public void setDefault(DatabaseDialect dialect) {
        this.defaultDialect = dialect;
    }

    /**
     * 按数据库类型设置默认方言。
     *
     * @param type 数据库类型，不可为 {@code null}
     * @throws IllegalArgumentException 该类型未注册
     */
    public void setDefault(DatabaseType type) {
        DatabaseDialect dialect = dialects.get(type);
        if (dialect == null) {
            throw new IllegalArgumentException("未注册方言：" + type);
        }
        this.defaultDialect = dialect;
    }

    /**
     * 按数据库产品名（与版本）探测方言，未知时回退默认方言。
     *
     * @param productName 产品名（来自 {@code DatabaseMetaData#getDatabaseProductName()}），可为 {@code null}
     * @param version     版本，可为 {@code null}
     * @return 方言，不可为 {@code null}
     */
    public DatabaseDialect detectByProduct(@Nullable String productName, @Nullable String version) {
        DatabaseType type = JdbcDialectDetector.detect(productName, version);
        DatabaseDialect dialect = dialects.get(type);
        return dialect != null ? dialect : getDefault();
    }
}
