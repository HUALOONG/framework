package cn.jowen.framework.data.core.query;

import cn.jowen.framework.data.core.sort.Direction;
import cn.jowen.framework.data.core.sort.Sort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;

class QueryWrapperTest {

    @Test
    void eq_condition() {
        QueryWrapper<ResolvableEntity> w = new QueryWrapper<>();
        w.eq(res("id"), 1L);

        assertThat(w.getConditions()).hasSize(1);
        assertThat(w.getConditions().get(0).getOperator()).isEqualTo(Operator.EQ);
        assertThat(w.getConditions().get(0).getColumn()).isEqualTo("id");
        assertThat(w.getConditions().get(0).getValue()).isEqualTo(1L);
    }

    @Test
    void multipleConditions() {
        QueryWrapper<ResolvableEntity> w = new QueryWrapper<>();
        w.eq(res("name"), "Alice")
         .gt(res("age"), 18)
         .lt(res("age"), 60);

        assertThat(w.getConditions()).hasSize(3);
        assertThat(w.getConditions().get(0).getOperator()).isEqualTo(Operator.EQ);
        assertThat(w.getConditions().get(1).getOperator()).isEqualTo(Operator.GT);
        assertThat(w.getConditions().get(2).getOperator()).isEqualTo(Operator.LT);
    }

    @Test
    void like_likeLeft_likeRight() {
        QueryWrapper<ResolvableEntity> w = new QueryWrapper<>();
        w.like(res("name"), "test")
         .likeLeft(res("name"), "test")
         .likeRight(res("name"), "test");

        assertThat(w.getConditions().get(0).getOperator()).isEqualTo(Operator.LIKE);
        assertThat(w.getConditions().get(1).getOperator()).isEqualTo(Operator.LIKE_LEFT);
        assertThat(w.getConditions().get(2).getOperator()).isEqualTo(Operator.LIKE_RIGHT);
    }

    @Test
    void in_notIn_between() {
        QueryWrapper<ResolvableEntity> w = new QueryWrapper<>();
        w.in(res("id"), List.of(1, 2, 3))
         .notIn(res("id"), List.of(4, 5))
         .between(res("age"), 10, 20);

        assertThat(w.getConditions().get(0).getOperator()).isEqualTo(Operator.IN);
        assertThat(w.getConditions().get(1).getOperator()).isEqualTo(Operator.NOT_IN);
        assertThat(w.getConditions().get(2).getOperator()).isEqualTo(Operator.BETWEEN);
        assertThat((Object[]) w.getConditions().get(2).getValue()).containsExactly(10, 20);
    }

    @Test
    void isNull_isNotNull() {
        QueryWrapper<ResolvableEntity> w = new QueryWrapper<>();
        w.isNull(res("email"))
         .isNotNull(res("name"));

        assertThat(w.getConditions().get(0).getOperator()).isEqualTo(Operator.IS_NULL);
        assertThat(w.getConditions().get(1).getOperator()).isEqualTo(Operator.IS_NOT_NULL);
    }

    @Test
    void addCondition() {
        QueryWrapper<ResolvableEntity> w = new QueryWrapper<>();
        w.addCondition(new Condition(Operator.EQ, "custom", "val"));
        assertThat(w.getConditions()).hasSize(1);
        assertThat(w.getConditions().get(0).getColumn()).isEqualTo("custom");
    }

    @Test
    void and_chain() {
        QueryWrapper<ResolvableEntity> w1 = new QueryWrapper<>();
        w1.eq(res("name"), "Alice");

        QueryWrapper<ResolvableEntity> w2 = new QueryWrapper<>();
        w2.eq(res("age"), 25);

        w1.and(w2);
        assertThat(w1.getConditions()).hasSize(2);
    }

    @Test
    void or_withExistingConditions() {
        QueryWrapper<ResolvableEntity> w1 = new QueryWrapper<>();
        w1.eq(res("name"), "Alice");

        QueryWrapper<ResolvableEntity> w2 = new QueryWrapper<>();
        w2.eq(res("age"), 25);

        w1.or(w2);
        assertThat(w1.getConditions()).hasSize(4);
        assertThat(w1.getConditions().get(1).getOperator()).isEqualTo(Operator.AND);
        assertThat(w1.getConditions().get(2).getOperator()).isEqualTo(Operator.OR);
        assertThat(w1.getConditions().get(3).getOperator()).isEqualTo(Operator.EQ);
    }

    @Test
    void or_withoutExistingConditions() {
        QueryWrapper<ResolvableEntity> w1 = new QueryWrapper<>();

        QueryWrapper<ResolvableEntity> w2 = new QueryWrapper<>();
        w2.eq(res("age"), 25);

        w1.or(w2);
        assertThat(w1.getConditions()).hasSize(2);
        assertThat(w1.getConditions().get(0).getOperator()).isEqualTo(Operator.OR);
        assertThat(w1.getConditions().get(1).getOperator()).isEqualTo(Operator.EQ);
    }

    @Test
    void joins() {
        QueryWrapper<ResolvableEntity> w = new QueryWrapper<>();
        w.innerJoin(res("order_id"), OtherEntity.class, res("id"))
         .leftJoin(res("user_id"), OtherEntity.class, res("id"))
         .rightJoin(res("role_id"), OtherEntity.class, res("id"));

        assertThat(w.getJoins()).hasSize(3);
        assertThat(w.getJoins().get(0).type()).isEqualTo(JoinType.INNER);
        assertThat(w.getJoins().get(1).type()).isEqualTo(JoinType.LEFT);
        assertThat(w.getJoins().get(2).type()).isEqualTo(JoinType.RIGHT);
    }

    @Test
    void groupBy() {
        QueryWrapper<ResolvableEntity> w = new QueryWrapper<>();
        w.groupBy(res("name"), res("age"));
        assertThat(w.getGroupBy()).containsExactly("name", "age");
    }

    @Test
    void orderBy() {
        QueryWrapper<ResolvableEntity> w = new QueryWrapper<>();
        w.orderByAsc(res("name"))
         .orderByDesc(res("age"));
        assertThat(w.getOrderBy()).containsExactly("name ASC", "age DESC");
    }

    @Test
    void orderBy_withSort() {
        QueryWrapper<ResolvableEntity> w = new QueryWrapper<>();
        Sort sort = Sort.by("name", Direction.ASC).and(Sort.by("age", Direction.DESC));
        w.orderBy(sort);
        assertThat(w.getOrderBy()).containsExactly("name ASC", "age DESC");
    }

    @Test
    void limit_and_offset() {
        QueryWrapper<ResolvableEntity> w = new QueryWrapper<>();
        w.limit(10).offset(20);
        assertThat(w.getLimit()).isEqualTo(10);
        assertThat(w.getOffset()).isEqualTo(20);
    }

    @Test
    void conditionsAndJoins_unmodifiable() {
        QueryWrapper<ResolvableEntity> w = new QueryWrapper<>();
        w.eq(res("id"), 1L);
        assertThatThrownBy(() -> w.getConditions().add(new Condition(Operator.EQ, "x", "y")))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    static class ResolvableEntity {}
    static class OtherEntity {}

    static Function<ResolvableEntity, ?> res(String column) {
        return new QueryWrapper.ResolvableFunction<>() {
            @Override public Object apply(ResolvableEntity t) { return null; }
            @Override public String getColumnName() { return column; }
        };
    }

    static Function<OtherEntity, ?> resOther(String column) {
        return new QueryWrapper.ResolvableFunction<>() {
            @Override public Object apply(OtherEntity t) { return null; }
            @Override public String getColumnName() { return column; }
        };
    }
}
