package cn.jowen.framework.data.mybatis.config;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * MyBatis Flex 配置属性。所有属性均有安全默认值，业务方通过 SPI 或配置绑定注入。
 * <p>
 * 配置前缀：{@code framework.data.mybatis}
 * </p>
 *
 * @author 王飞
 * @since 2026-08-26
 */
@NullMarked
public class MybatisFlexProperties {

    /** 数据源 URL */
    private String url = "";
    /** 数据源用户名 */
    private String username = "";
    /** 数据源密码 */
    private @Nullable String password;
    /** 驱动类名（可空，由 URL 推断） */
    private @Nullable String driverClassName;
    /** 主连接池最大连接数 */
    private int maximumPoolSize = 20;
    /** 连接超时毫秒 */
    private long connectionTimeout = 5000L;
    /** 映射文件位置 */
    private String mapperLocations = "classpath*:/mapper/**/*Mapper.xml";
    /** 类型别名包 */
    private String typeAliasesPackage = "";
    /** 数据审计开关 */
    private boolean auditEnabled = false;
    /** 字段加密开关 */
    private boolean encryptEnabled = false;
    /** 多租户开关 */
    private boolean tenantEnabled = false;
    /** SQL 审计日志开关 */
    private boolean sqlAuditEnabled = false;
    /** 逻辑删除开关 */
    private boolean logicDeleteEnabled = false;
    /** 乐观锁开关 */
    private boolean optimisticLockEnabled = false;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public @Nullable String getPassword() { return password; }
    public void setPassword(@Nullable String password) { this.password = password; }
    public @Nullable String getDriverClassName() { return driverClassName; }
    public void setDriverClassName(@Nullable String driverClassName) { this.driverClassName = driverClassName; }
    public int getMaximumPoolSize() { return maximumPoolSize; }
    public void setMaximumPoolSize(int maximumPoolSize) { this.maximumPoolSize = maximumPoolSize; }
    public long getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(long connectionTimeout) { this.connectionTimeout = connectionTimeout; }
    public String getMapperLocations() { return mapperLocations; }
    public void setMapperLocations(String mapperLocations) { this.mapperLocations = mapperLocations; }
    public String getTypeAliasesPackage() { return typeAliasesPackage; }
    public void setTypeAliasesPackage(String typeAliasesPackage) { this.typeAliasesPackage = typeAliasesPackage; }
    public boolean isAuditEnabled() { return auditEnabled; }
    public void setAuditEnabled(boolean auditEnabled) { this.auditEnabled = auditEnabled; }
    public boolean isEncryptEnabled() { return encryptEnabled; }
    public void setEncryptEnabled(boolean encryptEnabled) { this.encryptEnabled = encryptEnabled; }
    public boolean isTenantEnabled() { return tenantEnabled; }
    public void setTenantEnabled(boolean tenantEnabled) { this.tenantEnabled = tenantEnabled; }
    public boolean isSqlAuditEnabled() { return sqlAuditEnabled; }
    public void setSqlAuditEnabled(boolean sqlAuditEnabled) { this.sqlAuditEnabled = sqlAuditEnabled; }
    public boolean isLogicDeleteEnabled() { return logicDeleteEnabled; }
    public void setLogicDeleteEnabled(boolean logicDeleteEnabled) { this.logicDeleteEnabled = logicDeleteEnabled; }
    public boolean isOptimisticLockEnabled() { return optimisticLockEnabled; }
    public void setOptimisticLockEnabled(boolean optimisticLockEnabled) { this.optimisticLockEnabled = optimisticLockEnabled; }
}
