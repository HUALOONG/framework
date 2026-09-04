package cn.jowen.framework.data.core.support;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AuditableEntity} 测试。
 */
class AuditableEntityTest {

    static final class TestAuditable extends AuditableEntity<String> {
        private String id;

        @Override
        public String getId() {
            return id;
        }

        @Override
        public void setId(String id) {
            this.id = id;
        }
    }

    @Test
    void settersAndGetters() {
        TestAuditable e = new TestAuditable();
        Instant now = Instant.now();
        e.setCreatedBy("alice");
        e.setCreateTime(now);
        e.setUpdatedBy("bob");
        e.setUpdateTime(now.plusSeconds(10));

        assertThat(e.getCreatedBy()).isEqualTo("alice");
        assertThat(e.getCreateTime()).isEqualTo(now);
        assertThat(e.getUpdatedBy()).isEqualTo("bob");
        assertThat(e.getUpdateTime()).isEqualTo(now.plusSeconds(10));
    }

    @Test
    void nullableFields_defaultNull() {
        TestAuditable e = new TestAuditable();
        assertThat(e.getCreatedBy()).isNull();
        assertThat(e.getUpdatedBy()).isNull();
    }
}
