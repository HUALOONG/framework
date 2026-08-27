package cn.jowen.framework.data.core.query;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;

class UpdateWrapperTest {

    @Test
    void set_clause() {
        UpdateWrapper<Entity> w = new UpdateWrapper<>();
        w.set(res("status"), "ACTIVE");

        assertThat(w.getSetClauses()).hasSize(1);
        assertThat(w.getSetClauses().getFirst().column()).isEqualTo("status");
        assertThat(w.getSetClauses().getFirst().value()).isEqualTo("ACTIVE");
    }

    @Test
    void multipleSet() {
        UpdateWrapper<Entity> w = new UpdateWrapper<>();
        w.set(res("status"), "ACTIVE")
         .set(res("version"), 2);

        assertThat(w.getSetClauses()).hasSize(2);
    }

    @Test
    void eq_condition() {
        UpdateWrapper<Entity> w = new UpdateWrapper<>();
        w.eq(res("id"), 1L);

        assertThat(w.getConditions()).hasSize(1);
        assertThat(w.getConditions().get(0).getOperator()).isEqualTo(Operator.EQ);
        assertThat(w.getConditions().get(0).getColumn()).isEqualTo("id");
        assertThat(w.getConditions().get(0).getValue()).isEqualTo(1L);
    }

    @Test
    void ne_condition() {
        UpdateWrapper<Entity> w = new UpdateWrapper<>();
        w.ne(res("status"), "DELETED");

        assertThat(w.getConditions().getFirst().getOperator()).isEqualTo(Operator.NE);
    }

    @Test
    void in_condition() {
        UpdateWrapper<Entity> w = new UpdateWrapper<>();
        w.in(res("id"), List.of(1, 2, 3));

        assertThat(w.getConditions().getFirst().getOperator()).isEqualTo(Operator.IN);
        assertThat(w.getConditions().getFirst().getValue()).isEqualTo(List.of(1, 2, 3));
    }

    @Test
    void addCondition() {
        UpdateWrapper<Entity> w = new UpdateWrapper<>();
        w.addCondition(new Condition(Operator.GT, "age", 18));

        assertThat(w.getConditions()).hasSize(1);
        assertThat(w.getConditions().getFirst().getOperator()).isEqualTo(Operator.GT);
        assertThat(w.getConditions().getFirst().getColumn()).isEqualTo("age");
    }

    @Test
    void setWithNullValue() {
        UpdateWrapper<Entity> w = new UpdateWrapper<>();
        w.set(res("name"), null);

        assertThat(w.getSetClauses().getFirst().value()).isNull();
    }

    @Test
    void setClauses_unmodifiable() {
        UpdateWrapper<Entity> w = new UpdateWrapper<>();
        w.set(res("status"), "ACTIVE");
        assertThatThrownBy(() -> w.getSetClauses().add(new UpdateWrapper.SetClause("x", "y")))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void conditions_unmodifiable() {
        UpdateWrapper<Entity> w = new UpdateWrapper<>();
        w.eq(res("id"), 1L);
        assertThatThrownBy(() -> w.getConditions().add(new Condition(Operator.EQ, "x", "y")))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    static class Entity {}

    static Function<Entity, ?> res(String column) {
        return new QueryWrapper.ResolvableFunction<>() {
            @Override public Object apply(Entity t) { return null; }
            @Override public String getColumnName() { return column; }
        };
    }
}
