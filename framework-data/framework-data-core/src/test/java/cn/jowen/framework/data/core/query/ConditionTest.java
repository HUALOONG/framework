package cn.jowen.framework.data.core.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ConditionTest {

    @Test
    void basic_condition() {
        Condition cond = new Condition(Operator.EQ, "name", "Alice");
        assertThat(cond.getOperator()).isEqualTo(Operator.EQ);
        assertThat(cond.getColumn()).isEqualTo("name");
        assertThat(cond.getValue()).isEqualTo("Alice");
    }

    @Test
    void condition_withNullValue() {
        Condition cond = new Condition(Operator.IS_NULL, "email", null);
        assertThat(cond.getOperator()).isEqualTo(Operator.IS_NULL);
        assertThat(cond.getColumn()).isEqualTo("email");
        assertThat(cond.getValue()).isNull();
    }

    @Test
    void toString_format() {
        Condition cond = new Condition(Operator.EQ, "name", "Alice");
        assertThat(cond.toString()).isEqualTo("EQ(name, Alice)");
    }

    @Test
    void toString_withNull() {
        Condition cond = new Condition(Operator.IS_NULL, "email", null);
        assertThat(cond.toString()).isEqualTo("IS_NULL(email, null)");
    }
}
