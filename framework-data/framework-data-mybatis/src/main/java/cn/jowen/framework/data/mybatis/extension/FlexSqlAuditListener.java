package cn.jowen.framework.data.mybatis.extension;

import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class FlexSqlAuditListener implements ExtensionRegistry.Extension {

    private static final Logger logger = LoggerFactory.getLogger(FlexSqlAuditListener.class);

    private long slowThresholdMs = 500L;
    private boolean fullSqlEnabled = false;

    public FlexSqlAuditListener() {}
    public FlexSqlAuditListener(long slowThresholdMs, boolean fullSqlEnabled) {
        this.slowThresholdMs = slowThresholdMs;
        this.fullSqlEnabled = fullSqlEnabled;
    }

    @Override public String name() { return "sqlAudit"; }
    @Override public int order() { return 500; }

    public void onExecute(String sql, Object[] params, long durationMs) {
        if (durationMs >= slowThresholdMs) {
            logger.warn("[SLOW-SQL] " + durationMs + "ms: " + sql + " " + java.util.Arrays.toString(params));
        }
        if (fullSqlEnabled) {
            logger.info("[SQL-AUDIT] " + durationMs + "ms: " + sql);
        }
    }

    public void onError(String sql, Object[] params, String error) {
        logger.error("[SQL-ERROR] " + error + " | " + sql);
    }

    public long getSlowThresholdMs() { return slowThresholdMs; }
    public void setSlowThresholdMs(long v) { this.slowThresholdMs = v; }
    public boolean isFullSqlEnabled() { return fullSqlEnabled; }
    public void setFullSqlEnabled(boolean v) { this.fullSqlEnabled = v; }
}
