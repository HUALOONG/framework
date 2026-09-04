package cn.jowen.framework.data.mybatis.extension;

import cn.jowen.framework.data.mybatis.exception.FlexOptimisticLockException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlexOptimisticLockHandlerTest {

    private final FlexOptimisticLockHandler handler = new FlexOptimisticLockHandler();

    @Test
    void checkUpdateResult_zeroRows_withVersion_throws() {
        VersionedEntity entity = new VersionedEntity();
        entity.version = 1;
        assertThatThrownBy(() -> handler.checkUpdateResult(entity, 0))
                .isInstanceOf(FlexOptimisticLockException.class);
    }

    @Test
    void checkUpdateResult_zeroRows_noVersion_noThrow() {
        Object entity = new Object();
        assertThatCode(() -> handler.checkUpdateResult(entity, 0)).doesNotThrowAnyException();
    }

    @Test
    void checkUpdateResult_nonZeroRows_noThrow() {
        VersionedEntity entity = new VersionedEntity();
        entity.version = 1;
        assertThatCode(() -> handler.checkUpdateResult(entity, 1)).doesNotThrowAnyException();
    }

    @Test
    void incrementVersion() {
        VersionedEntity entity = new VersionedEntity();
        entity.version = 5;
        handler.incrementVersion(entity);
        assertThat(entity.version).isEqualTo(6);
    }

    @Test
    void name() {
        assertThat(handler.name()).isEqualTo("optimisticLock");
    }

    @Test
    void order() {
        assertThat(handler.order()).isEqualTo(400);
    }

    @Test
    void incrementVersion_longType() {
        LongEntity entity = new LongEntity();
        entity.version = 5L;
        handler.incrementVersion(entity);
        assertThat(entity.version).isEqualTo(6L);
    }

    @Test
    void incrementVersion_nullVersion_noop() {
        NullVersionEntity entity = new NullVersionEntity();
        entity.version = null;
        handler.incrementVersion(entity);
        assertThat(entity.version).isNull();
    }

    @Test
    void incrementVersion_noSetter_noop() {
        NoSetterEntity entity = new NoSetterEntity();
        entity.version = 1;
        handler.incrementVersion(entity);
        assertThat(entity.version).isEqualTo(1);
    }

    @Test
    void incrementVersion_noVersionField_catchesException() {
        // entity without a getVersion method -> reflection NPE is swallowed
        assertThatCode(() -> handler.incrementVersion(new Object())).doesNotThrowAnyException();
    }

    static class VersionedEntity {
        Integer version;
        Integer getVersion() { return version; }
        void setVersion(Integer v) { this.version = v; }
    }

    static class LongEntity {
        Long version;
        Long getVersion() { return version; }
        void setVersion(Long v) { this.version = v; }
    }

    static class NullVersionEntity {
        Integer version;
        Integer getVersion() { return version; }
        void setVersion(Integer v) { this.version = v; }
    }

    static class NoSetterEntity {
        Integer version;
        Integer getVersion() { return version; }
    }

    @Test
    void incrementVersion_versionInSuperclass() {
        SubVersionedEntity entity = new SubVersionedEntity();
        entity.version = 3;
        handler.incrementVersion(entity);
        assertThat(entity.version).isEqualTo(4);
    }

    static class BaseVersioned {
        Integer version;
        Integer getVersion() { return version; }
        void setVersion(Integer v) { this.version = v; }
    }

    static class SubVersionedEntity extends BaseVersioned {
    }
}
