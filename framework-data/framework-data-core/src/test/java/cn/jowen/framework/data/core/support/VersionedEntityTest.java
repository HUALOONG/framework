package cn.jowen.framework.data.core.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VersionedEntityTest {

    /** 不重写版本访问器，直接验证基类默认实现（乐观锁版本号内部托管）。 */
    static class PlainEntity extends VersionedEntity<String> {
        @Override
        public String getId() {
            return null;
        }

        @Override
        public void setId(String id) {
        }
    }

    @Test
    void baseNoOpVersionAccessors() {
        PlainEntity entity = new PlainEntity();
        // 基类默认版本号为 0
        assertThat(entity.getVersion()).isEqualTo(0);
        // setVersion 为内部无操作，调用不应抛异常
        entity.setVersion(5);
        assertThat(entity.getVersion()).isEqualTo(0);
        // incrementVersion 内部调用 setVersion(getVersion() + 1)
        entity.incrementVersion();
        assertThat(entity.getVersion()).isEqualTo(0);
    }
}
