package cn.jowen.framework.extras.web.operatelog;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class OperateLogEventTest {

    @Test
    void record_exposesComponentsAndValueEquality() {
        Instant now = Instant.now();
        OperateLogEvent e1 = new OperateLogEvent("admin", "order", "create", "A#b",
                "[1]", "ok", true, null, 5L, now);
        OperateLogEvent e2 = new OperateLogEvent("admin", "order", "create", "A#b",
                "[1]", "ok", true, null, 5L, now);

        assertThat(e1.operator()).isEqualTo("admin");
        assertThat(e1.module()).isEqualTo("order");
        assertThat(e1.operation()).isEqualTo("create");
        assertThat(e1.method()).isEqualTo("A#b");
        assertThat(e1.params()).isEqualTo("[1]");
        assertThat(e1.result()).isEqualTo("ok");
        assertThat(e1.success()).isTrue();
        assertThat(e1.error()).isNull();
        assertThat(e1.costMillis()).isEqualTo(5L);
        assertThat(e1.timestamp()).isEqualTo(now);

        assertThat(e1).isEqualTo(e2);
        assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
        assertThat(e1.toString()).contains("admin", "order", "create");
    }
}
