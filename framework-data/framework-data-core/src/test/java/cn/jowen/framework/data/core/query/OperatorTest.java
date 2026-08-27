package cn.jowen.framework.data.core.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class OperatorTest {

    @Test
    void enumValues() {
        Operator[] values = Operator.values();
        assertThat(values).containsExactly(
            Operator.EQ,
            Operator.NE,
            Operator.LIKE,
            Operator.LIKE_LEFT,
            Operator.LIKE_RIGHT,
            Operator.GT,
            Operator.LT,
            Operator.GTE,
            Operator.LTE,
            Operator.IN,
            Operator.NOT_IN,
            Operator.BETWEEN,
            Operator.IS_NULL,
            Operator.IS_NOT_NULL,
            Operator.NOT,
            Operator.AND,
            Operator.OR
        );
    }
}
