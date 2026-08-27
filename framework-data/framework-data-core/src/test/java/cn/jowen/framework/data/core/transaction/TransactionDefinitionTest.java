package cn.jowen.framework.data.core.transaction;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TransactionDefinitionTest {

    @Test
    void defaults() {
        TransactionDefinition def = TransactionDefinition.defaults();
        assertThat(def.getPropagation()).isEqualTo(TransactionDefinition.Propagation.REQUIRED);
        assertThat(def.getIsolation()).isEqualTo(TransactionDefinition.Isolation.DEFAULT);
        assertThat(def.getTimeout()).isEqualTo(-1);
        assertThat(def.isReadOnly()).isFalse();
    }

    @Test
    void customDefinition() {
        TransactionDefinition def = new TransactionDefinition(
            TransactionDefinition.Propagation.REQUIRES_NEW,
            TransactionDefinition.Isolation.SERIALIZABLE,
            30,
            true
        );

        assertThat(def.getPropagation()).isEqualTo(TransactionDefinition.Propagation.REQUIRES_NEW);
        assertThat(def.getIsolation()).isEqualTo(TransactionDefinition.Isolation.SERIALIZABLE);
        assertThat(def.getTimeout()).isEqualTo(30);
        assertThat(def.isReadOnly()).isTrue();
    }

    @Test
    void propagationEnumValues() {
        TransactionDefinition.Propagation[] values = TransactionDefinition.Propagation.values();
        assertThat(values).containsExactly(
            TransactionDefinition.Propagation.REQUIRED,
            TransactionDefinition.Propagation.REQUIRES_NEW,
            TransactionDefinition.Propagation.NESTED,
            TransactionDefinition.Propagation.SUPPORTS,
            TransactionDefinition.Propagation.NOT_SUPPORTED,
            TransactionDefinition.Propagation.NEVER,
            TransactionDefinition.Propagation.MANDATORY
        );
    }

    @Test
    void isolationEnumValues() {
        TransactionDefinition.Isolation[] values = TransactionDefinition.Isolation.values();
        assertThat(values).containsExactly(
            TransactionDefinition.Isolation.DEFAULT,
            TransactionDefinition.Isolation.READ_UNCOMMITTED,
            TransactionDefinition.Isolation.READ_COMMITTED,
            TransactionDefinition.Isolation.REPEATABLE_READ,
            TransactionDefinition.Isolation.SERIALIZABLE
        );
    }
}
