package cn.jowen.framework.data.core.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 测试 {@link TransactionDefinition} 的构造、默认值与枚举完整性。
 */
class TransactionDefinitionTest {

    @Test
    void constructorExposesFields() {
        TransactionDefinition def = new TransactionDefinition(
                TransactionDefinition.Propagation.REQUIRES_NEW,
                TransactionDefinition.Isolation.SERIALIZABLE, 30, true);
        assertThat(def.getPropagation()).isEqualTo(TransactionDefinition.Propagation.REQUIRES_NEW);
        assertThat(def.getIsolation()).isEqualTo(TransactionDefinition.Isolation.SERIALIZABLE);
        assertThat(def.getTimeout()).isEqualTo(30);
        assertThat(def.isReadOnly()).isTrue();
    }

    @Test
    void defaultsAreRequiredDefaultNoTimeoutNotReadOnly() {
        TransactionDefinition def = TransactionDefinition.defaults();
        assertThat(def.getPropagation()).isEqualTo(TransactionDefinition.Propagation.REQUIRED);
        assertThat(def.getIsolation()).isEqualTo(TransactionDefinition.Isolation.DEFAULT);
        assertThat(def.getTimeout()).isEqualTo(-1);
        assertThat(def.isReadOnly()).isFalse();
    }

    @Test
    void propagationHasSevenValues() {
        assertThat(TransactionDefinition.Propagation.values()).containsExactly(
                TransactionDefinition.Propagation.REQUIRED,
                TransactionDefinition.Propagation.REQUIRES_NEW,
                TransactionDefinition.Propagation.NESTED,
                TransactionDefinition.Propagation.SUPPORTS,
                TransactionDefinition.Propagation.NOT_SUPPORTED,
                TransactionDefinition.Propagation.NEVER,
                TransactionDefinition.Propagation.MANDATORY);
    }

    @Test
    void isolationHasFiveValues() {
        assertThat(TransactionDefinition.Isolation.values()).containsExactly(
                TransactionDefinition.Isolation.DEFAULT,
                TransactionDefinition.Isolation.READ_UNCOMMITTED,
                TransactionDefinition.Isolation.READ_COMMITTED,
                TransactionDefinition.Isolation.REPEATABLE_READ,
                TransactionDefinition.Isolation.SERIALIZABLE);
    }
}
