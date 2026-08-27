package cn.jowen.framework.data.core.sort;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class NullHandlingTest {

    @Test
    void enumValues() {
        NullHandling[] values = NullHandling.values();
        assertThat(values).containsExactly(NullHandling.NULLS_FIRST, NullHandling.NULLS_LAST);
    }
}
