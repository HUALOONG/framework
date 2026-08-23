package cn.jowen.framework.data.jdbc.dialect;

import cn.jowen.framework.data.core.dialect.DatabaseType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 方言探测器：依据数据库产品名或 JDBC URL 推断 {@link DatabaseType}。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class JdbcDialectDetector {

    private JdbcDialectDetector() {
    }

    /**
     * 按产品名探测数据库类型。
     *
     * @param productName 产品名，可为 {@code null}
     * @param version     版本，可为 {@code null}（预留，当前未用于推断）
     * @return 数据库类型，未知返回 {@link DatabaseType#UNKNOWN}
     */
    public static DatabaseType detect(@Nullable String productName, @Nullable String version) {
        if (productName == null) {
            return DatabaseType.UNKNOWN;
        }
        String p = productName.toLowerCase();
        if (p.contains("h2")) {
            return DatabaseType.H2;
        }
        if (p.contains("mysql") || p.contains("maria")) {
            return DatabaseType.MYSQL;
        }
        if (p.contains("postgre")) {
            return DatabaseType.POSTGRESQL;
        }
        if (p.contains("oracle")) {
            return DatabaseType.ORACLE;
        }
        if (p.contains("microsoft") || p.contains("sql server") || p.contains("sqlserver")) {
            return DatabaseType.SQLSERVER;
        }
        return DatabaseType.UNKNOWN;
    }

    /**
     * 按 JDBC URL 探测数据库类型。
     *
     * @param url JDBC URL，可为 {@code null}
     * @return 数据库类型，未知返回 {@link DatabaseType#UNKNOWN}
     */
    public static DatabaseType detectByUrl(@Nullable String url) {
        if (url == null) {
            return DatabaseType.UNKNOWN;
        }
        String u = url.toLowerCase();
        if (u.contains(":h2:")) {
            return DatabaseType.H2;
        }
        if (u.contains(":mysql:") || u.contains("mariadb")) {
            return DatabaseType.MYSQL;
        }
        if (u.contains(":postgre:")) {
            return DatabaseType.POSTGRESQL;
        }
        if (u.contains(":oracle:")) {
            return DatabaseType.ORACLE;
        }
        if (u.contains(":sqlserver:") || u.contains(":microsoft:")) {
            return DatabaseType.SQLSERVER;
        }
        return DatabaseType.UNKNOWN;
    }
}
