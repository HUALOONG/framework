package cn.jowen.framework.data.core.transaction;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TransactionDefinitionTest {

    @Test
    void defaults() {
        TransactionDefinition def = TransactionDefinition.defaults();
        assertThat(def.getPropagation()).isEqualTo(Propagation.REQUIRED);
        assertThat(def.getIsolation()).isEqualTo(Isolation.DEFAULT);
        assertThat(def.getTimeout()).isEqualTo(-1);
        assertThat(def.isReadOnly()).isFalse();
    }

    @Test
    void customDefinition() {
        TransactionDefinition def = new TransactionDefinition(
            Propagation.REQUIRES_NEW,
            Isolation.SERIALIZABLE,
            30,
            true
        );

        assertThat(def.getPropagation()).isEqualTo(Propagation.REQUIRES_NEW);
        assertThat(def.getIsolation()).isEqualTo(Isolation.SERIALIZABLE);
        assertThat(def.getTimeout()).isEqualTo(30);
        assertThat(def.isReadOnly()).isTrue();
    }

    @Test
    void propagationEnumValues() {
        Propagation[] values = Propagation.values();
        assertThat(values).containsExactly(
            Propagation.REQUIRED,
            Propagation.REQUIRES_NEW,
            Propagation.NESTED,
            Propagation.SUPPORTS,
            Propagation.NOT_SUPPORTED,
            Propagation.NEVER,
            Propagation.MANDATORY
        );
    }

    @Test
    void isolationEnumValues() {
        Isolation[] values = Isolation.values();
        assertThat(values).containsExactly(
            Isolation.DEFAULT,
            Isolation.READ_UNCOMMITTED,
            Isolation.READ_COMMITTED,
            Isolation.REPEATABLE_READ,
            Isolation.SERIALIZABLE
        );
    }
}

