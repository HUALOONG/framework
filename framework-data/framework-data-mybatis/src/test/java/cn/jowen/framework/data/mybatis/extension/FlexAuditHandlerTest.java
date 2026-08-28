package cn.jowen.framework.data.mybatis.extension;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link FlexAuditHandler} 单元测试：验证快照 JSON 与审计记录落库出口。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class FlexAuditHandlerTest {

    record DemoEntity(String name, int age) {
    }

    @Test
    void onInsert_writesAfterSnapshot() {
        List<FlexAuditHandler.AuditRecord> records = new ArrayList<>();
        FlexAuditHandler handler = new FlexAuditHandler(records::add);

        handler.onInsert(new DemoEntity("alice", 30));

        assertThat(records).hasSize(1);
        FlexAuditHandler.AuditRecord record = records.get(0);
        assertThat(record.changeType()).isEqualTo(FlexAuditHandler.ChangeType.INSERT);
        assertThat(record.beforeJson()).isNull();
        assertThat(record.afterJson()).contains("\"name\":\"alice\"").contains("\"age\":30");
        assertThat(record.table()).isEqualTo("DemoEntity");
    }

    @Test
    void onDelete_keepsBeforeSnapshot() {
        List<FlexAuditHandler.AuditRecord> records = new ArrayList<>();
        FlexAuditHandler handler = new FlexAuditHandler(records::add);

        handler.onDelete(new DemoEntity("bob", 25));

        assertThat(records).hasSize(1);
        FlexAuditHandler.AuditRecord record = records.get(0);
        assertThat(record.changeType()).isEqualTo(FlexAuditHandler.ChangeType.DELETE);
        assertThat(record.beforeJson()).contains("\"name\":\"bob\"");
        assertThat(record.afterJson()).isNull();
    }

    @Test
    void defaultSink_discardsRecords() {
        FlexAuditHandler handler = new FlexAuditHandler();
        handler.onInsert(new DemoEntity("carol", 40));
        // 默认 sink 为丢弃实现，仅验证不抛异常
        assertThat(handler.name()).isEqualTo("audit");
    }
}