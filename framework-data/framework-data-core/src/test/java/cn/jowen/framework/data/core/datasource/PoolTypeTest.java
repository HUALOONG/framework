package cn.jowen.framework.data.core.datasource;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class PoolTypeTest {

    @Test
    void enumValues() {
        PoolType[] values = PoolType.values();
        assertThat(values).containsExactly(PoolType.HIKARI, PoolType.DRUID, PoolType.NONE, PoolType.SIMPLE);
    }

    @Test
    void valueOf() {
        assertThat(PoolType.valueOf("HIKARI")).isEqualTo(PoolType.HIKARI);
        assertThat(PoolType.valueOf("DRUID")).isEqualTo(PoolType.DRUID);
        assertThat(PoolType.valueOf("NONE")).isEqualTo(PoolType.NONE);
        assertThat(PoolType.valueOf("SIMPLE")).isEqualTo(PoolType.SIMPLE);
    }
}
