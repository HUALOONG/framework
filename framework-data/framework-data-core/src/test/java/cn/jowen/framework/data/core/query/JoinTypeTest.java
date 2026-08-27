package cn.jowen.framework.data.core.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JoinTypeTest {

    @Test
    void enumValues() {
        JoinType[] values = JoinType.values();
        assertThat(values).containsExactly(
            JoinType.INNER,
            JoinType.LEFT,
            JoinType.RIGHT,
            JoinType.FULL,
            JoinType.CROSS
        );
    }
}
