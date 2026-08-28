package cn.jowen.framework.data.jdbc.config;

import cn.jowen.framework.data.core.datasource.DataSourceProperties;
import org.jspecify.annotations.NullMarked;

/**
 * JDBC 运行期配置属性（纯 POJO）。
 *
 * <p>数据源连接信息（url/username/password/driverClassName/连接池参数）复用 core 的
 * {@link DataSourceProperties}，本类只承载 JDBC 专有开关，
 * 避免重复定义同名属性类。boot 层通过继承本类（{@code BootJdbcProperties}）绑定 Spring 配置。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class JdbcProperties {

    /** 是否打印 SQL 日志，默认开启。 */
    private boolean sqlLogEnabled = true;

    /** 慢 SQL 阈值（毫秒），超过则打 WARN 日志，默认 500ms。 */
    private long slowSqlThreshold = 500L;

    /** 是否开启多租户，默认关闭。 */
    private boolean tenantEnabled = false;

    /** 多租户列名，默认 tenant_id。 */
    private String tenantColumn = "tenant_id";

    public JdbcProperties() {
    }

    public boolean isSqlLogEnabled() {
        return sqlLogEnabled;
    }

    public void setSqlLogEnabled(boolean sqlLogEnabled) {
        this.sqlLogEnabled = sqlLogEnabled;
    }

    public long getSlowSqlThreshold() {
        return slowSqlThreshold;
    }

    public void setSlowSqlThreshold(long slowSqlThreshold) {
        this.slowSqlThreshold = slowSqlThreshold;
    }

    public boolean isTenantEnabled() {
        return tenantEnabled;
    }

    public void setTenantEnabled(boolean tenantEnabled) {
        this.tenantEnabled = tenantEnabled;
    }

    public String getTenantColumn() {
        return tenantColumn;
    }

    public void setTenantColumn(String tenantColumn) {
        this.tenantColumn = tenantColumn;
    }
}
