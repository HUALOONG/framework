package cn.jowen.framework.data.core.support;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

/**
 * 实体基类测试。
 */
class EntityBaseTest {

    static class TestEntity extends EntityBase<Long> {
        private Long id;

        @Override public Long getId() { return id; }
        @Override public void setId(Long id) { this.id = id; }
    }

    @Test
    void entityBase_setAndGetId() {
        TestEntity entity = new TestEntity();
        entity.setId(1L);
        assertThat(entity.getId()).isEqualTo(1L);
    }

    static class TestVersioned extends VersionedEntity<Long> {
        private Long id;
        private int version;

        @Override public Long getId() { return id; }
        @Override public void setId(Long id) { this.id = id; }
        @Override public int getVersion() { return version; }
        @Override public void setVersion(int version) { this.version = version; }
    }

    @Test
    void versionedEntity_defaultVersion() {
        TestVersioned entity = new TestVersioned();
        assertThat(entity.getVersion()).isEqualTo(0);
    }

    @Test
    void versionedEntity_incrementVersion() {
        TestVersioned entity = new TestVersioned();
        entity.setVersion(5);
        entity.incrementVersion();
        assertThat(entity.getVersion()).isEqualTo(6);
    }

    static class TestAuditable extends AuditableEntity<Long> {
        private Long id;
        private String createdBy;
        private Instant createTime;
        private String updatedBy;
        private Instant updateTime;

        @Override public Long getId() { return id; }
        @Override public void setId(Long id) { this.id = id; }
        @Override public int getVersion() { return 0; }
        @Override public void setVersion(int version) {}
        @Override public String getCreatedBy() { return createdBy; }
        @Override public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
        @Override public Instant getCreateTime() { return createTime; }
        @Override public void setCreateTime(Instant createTime) { this.createTime = createTime; }
        @Override public String getUpdatedBy() { return updatedBy; }
        @Override public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
        @Override public Instant getUpdateTime() { return updateTime; }
        @Override public void setUpdateTime(Instant updateTime) { this.updateTime = updateTime; }
    }

    @Test
    void auditableEntity_defaultNull() {
        TestAuditable entity = new TestAuditable();
        assertThat(entity.getCreatedBy()).isNull();
        assertThat(entity.getUpdatedBy()).isNull();
        assertThat(entity.getCreateTime()).isNull();
        assertThat(entity.getUpdateTime()).isNull();
    }

    @Test
    void auditableEntity_setAndGet() {
        TestAuditable entity = new TestAuditable();
        Instant now = Instant.now();
        entity.setCreatedBy("admin");
        entity.setCreateTime(now);
        entity.setUpdatedBy("user1");
        entity.setUpdateTime(now);

        assertThat(entity.getCreatedBy()).isEqualTo("admin");
        assertThat(entity.getCreateTime()).isEqualTo(now);
        assertThat(entity.getUpdatedBy()).isEqualTo("user1");
        assertThat(entity.getUpdateTime()).isEqualTo(now);
    }
}
