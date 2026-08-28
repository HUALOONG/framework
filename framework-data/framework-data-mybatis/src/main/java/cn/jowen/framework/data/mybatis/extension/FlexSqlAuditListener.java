package cn.jowen.framework.data.mybatis.extension;

import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import org.jspecify.annotations.NullMarked;

@NullMarked
/**
 * 「FlexSqlAuditListener」封装相关能力。
 *
 * @author Jowen
 * @since 0.0.1
 * @version 0.0.1
 */
public class FlexSqlAuditListener implements ExtensionRegistry.Extension {

    /** logger 常量。 */
    private static final Logger logger = LoggerFactory.getLogger(FlexSqlAuditListener.class);

    /** slowThresholdMs 字段。 */
    private long slowThresholdMs = 500L;
    /** fullSqlEnabled 字段。 */
    private boolean fullSqlEnabled = false;

    /** public 字段。 */
    public FlexSqlAuditListener() {}
    /**
     * 构造实例。
     * @param slowThresholdMs 参数 slowThresholdMs
     * @param fullSqlEnabled 参数 fullSqlEnabled
     */
    public FlexSqlAuditListener(long slowThresholdMs, boolean fullSqlEnabled) {
        this.slowThresholdMs = slowThresholdMs;
        this.fullSqlEnabled = fullSqlEnabled;
    }

    /** return 字段。 */
    @Override public String name() { return "sqlAudit"; }
    /** return 字段。 */
    @Override public int order() { return 500; }

    /**
     * 执行on execute操作。
     * @param sql 参数 sql
     * @param durationMs 参数 durationMs
     */
    public void onExecute(String sql, Object[] params, long durationMs) {
        if (durationMs >= slowThresholdMs) {
            logger.warn("[SLOW-SQL] " + durationMs + "ms: " + sql + " " + java.util.Arrays.toString(params));
        }
        if (fullSqlEnabled) {
            logger.info("[SQL-AUDIT] " + durationMs + "ms: " + sql);
        }
    }

    /**
     * 执行on error操作。
     * @param sql 参数 sql
     * @param error 参数 error
     */
    public void onError(String sql, Object[] params, String error) {
        logger.error("[SQL-ERROR] " + error + " | " + sql);
    }

    /** slowThresholdMs 字段。 */
    public long getSlowThresholdMs() { return slowThresholdMs; }
    /** void 字段。 */
    public void setSlowThresholdMs(long v) { this.slowThresholdMs = v; }
    /** fullSqlEnabled 字段。 */
    public boolean isFullSqlEnabled() { return fullSqlEnabled; }
    /** void 字段。 */
    public void setFullSqlEnabled(boolean v) { this.fullSqlEnabled = v; }
}
