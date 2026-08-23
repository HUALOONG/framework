package cn.jowen.framework.data.mybatis.extension;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlexSqlAuditListenerTest {

    @Test
    void onExecute_slowSqlWarn() {
        FlexSqlAuditListener listener = new FlexSqlAuditListener(500, false);
        // Should not throw (slow SQL threshold met)
        listener.onExecute("SELECT * FROM users", new Object[]{1}, 1000);
    }

    @Test
    void onExecute_normalSql() {
        FlexSqlAuditListener listener = new FlexSqlAuditListener(500, false);
        listener.onExecute("SELECT * FROM users", new Object[]{1}, 10);
        // No assertion needed - just verify no exception
    }

    @Test
    void onExecute_fullSqlEnabled() {
        FlexSqlAuditListener listener = new FlexSqlAuditListener(500, true);
        listener.onExecute("SELECT * FROM users", new Object[]{1}, 1000);
    }

    @Test
    void onError() {
        FlexSqlAuditListener listener = new FlexSqlAuditListener();
        listener.onError("SELECT 1", new Object[]{}, "timeout");
        // Just verify no exception
    }

    @Test
    void getSetThreshold() {
        FlexSqlAuditListener listener = new FlexSqlAuditListener();
        listener.setSlowThresholdMs(1000);
        assertThat(listener.getSlowThresholdMs()).isEqualTo(1000);
    }
}
