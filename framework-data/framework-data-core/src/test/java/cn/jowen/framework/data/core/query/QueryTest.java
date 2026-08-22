package cn.jowen.framework.data.core.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.Collectors;

import cn.jowen.framework.data.core.page.Sort;

import org.junit.jupiter.api.Test;

/**
 * 测试 {@link Query} 的条件构造、排序/分页上限设置、顺序保持与条件列表隔离性。
 */
class QueryTest {

    @Test
    void emptyQueryHasNoConditionsUnsortedAndNoLimit() {
        Query query = Query.empty();
        assertThat(query.getConditions()).isEmpty();
        assertThat(query.getSort().isEmpty()).isTrue();
        assertThat(query.getLimit()).isNull();
    }

    @Test
    void eqAddsEqCondition() {
        Query q = Query.empty().eq("status", "ACTIVE");
        assertThat(q.getConditions()).hasSize(1);

        Query.Condition c = q.getConditions().get(0);
        assertThat(c.getOperator()).isEqualTo(Query.Operator.EQ);
        assertThat(c.getColumn()).isEqualTo("status");
        assertThat(c.getValue()).isEqualTo("ACTIVE");
    }

    @Test
    void neGtLikeAddCorrespondingConditions() {
        Query q = Query.empty().ne("age", 1).gt("score", 100).like("name", "%a%");
        List<Query.Condition> cs = q.getConditions();
        assertThat(cs).hasSize(3);
        assertThat(cs.get(0).getOperator()).isEqualTo(Query.Operator.NE);
        assertThat(cs.get(1).getOperator()).isEqualTo(Query.Operator.GT);
        assertThat(cs.get(2).getOperator()).isEqualTo(Query.Operator.LIKE);
        assertThat(cs.get(0).getValue()).isEqualTo(1);
        assertThat(cs.get(1).getValue()).isEqualTo(100);
        assertThat(cs.get(2).getValue()).isEqualTo("%a%");
    }

    @Test
    void conditionValueMayBeNull() {
        Query q = Query.empty().eq("deletedAt", null);
        assertThat(q.getConditions().get(0).getValue()).isNull();
    }

    @Test
    void conditionsPreserveInsertionOrder() {
        Query q = Query.empty().eq("a", 1).ne("b", 2).gt("c", 3).like("d", 4);
        List<Query.Operator> ops = q.getConditions().stream()
                .map(Query.Condition::getOperator)
                .collect(Collectors.toList());
        assertThat(ops).containsExactly(
                Query.Operator.EQ, Query.Operator.NE, Query.Operator.GT, Query.Operator.LIKE);
    }

    @Test
    void orderBySetsSort() {
        Sort sort = Sort.by("id", Sort.Direction.DESC);
        Query q = Query.empty().orderBy(sort);
        assertThat(q.getSort()).isSameAs(sort);
    }

    @Test
    void limitStoresValue() {
        Query q = Query.empty().limit(50);
        assertThat(q.getLimit()).isEqualTo(50);
    }

    @Test
    void getConditionsReturnsUnmodifiableCopy() {
        Query q = Query.empty().eq("x", 1);
        List<Query.Condition> cs = q.getConditions();
        assertThatThrownBy(() -> cs.add(new Query.Condition(Query.Operator.EQ, "y", 2)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void operatorEnumContainsAllDefinedConstants() {
        assertThat(Query.Operator.values()).containsExactlyInAnyOrder(
                Query.Operator.EQ, Query.Operator.NE, Query.Operator.LIKE, Query.Operator.GT,
                Query.Operator.LT, Query.Operator.GTE, Query.Operator.LTE, Query.Operator.IN);
    }
}
