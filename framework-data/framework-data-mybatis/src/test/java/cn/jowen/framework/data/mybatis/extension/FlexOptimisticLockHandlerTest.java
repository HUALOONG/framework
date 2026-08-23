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

    static class VersionedEntity {
        Integer version;
        Integer getVersion() { return version; }
        void setVersion(Integer v) { this.version = v; }
    }
}
