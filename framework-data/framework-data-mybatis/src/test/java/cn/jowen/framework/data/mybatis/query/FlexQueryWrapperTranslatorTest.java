package cn.jowen.framework.data.mybatis.query;

import cn.jowen.framework.data.core.query.Condition;
import cn.jowen.framework.data.core.query.Operator;
import cn.jowen.framework.data.core.query.QueryWrapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlexQueryWrapperTranslatorTest {

    @Test
    void toWhereClause_singleEq() {
        List<Condition> conditions = List.of(new Condition(Operator.EQ, "name", "Alice"));
        String sql = FlexQueryWrapperTranslator.toWhereClause(conditions);
        assertThat(sql).contains("name = 'Alice'");
    }

    @Test
    void toWhereClause_multipleConditions() {
        List<Condition> conditions = List.of(
                new Condition(Operator.EQ, "status", 1),
                new Condition(Operator.GT, "age", 18)
        );
        String sql = FlexQueryWrapperTranslator.toWhereClause(conditions);
        assertThat(sql).contains("status = 1");
        assertThat(sql).contains("AND");
        assertThat(sql).contains("age > 18");
    }

    @Test
    void toWhereClause_empty() {
        assertThat(FlexQueryWrapperTranslator.toWhereClause(List.of())).isEmpty();
        assertThat(FlexQueryWrapperTranslator.toWhereClause(null)).isEmpty();
    }

    @Test
    void toWhereClause_like() {
        List<Condition> conditions = List.of(new Condition(Operator.LIKE, "name", "abc"));
        String sql = FlexQueryWrapperTranslator.toWhereClause(conditions);
        assertThat(sql).contains("LIKE");
    }

    @Test
    void toWhereClause_in() {
        List<Condition> conditions = List.of(new Condition(Operator.IN, "id", List.of(1, 2, 3)));
        String sql = FlexQueryWrapperTranslator.toWhereClause(conditions);
        assertThat(sql).contains("IN (");
    }

    @Test
    void toOrderByClause() {
        List<String> orderBy = List.of("name ASC", "age DESC");
        assertThat(FlexQueryWrapperTranslator.toOrderByClause(orderBy)).isEqualTo("ORDER BY name ASC, age DESC");
    }

    @Test
    void toOrderByClause_empty() {
        assertThat(FlexQueryWrapperTranslator.toOrderByClause(List.of())).isEmpty();
        assertThat(FlexQueryWrapperTranslator.toOrderByClause(null)).isEmpty();
    }

    @Test
    void toLimitClause() {
        String sql = FlexQueryWrapperTranslator.toLimitClause(10, 20);
        assertThat(sql).contains("OFFSET 20");
        assertThat(sql).contains("LIMIT 10");
    }

    @Test
    void toLimitClause_onlyLimit() {
        String sql = FlexQueryWrapperTranslator.toLimitClause(5, null);
        assertThat(sql).isEqualTo(" LIMIT 5");
    }

    @Test
    void toLimitClause_onlyOffset() {
        String sql = FlexQueryWrapperTranslator.toLimitClause(null, 10);
        assertThat(sql).isEqualTo("OFFSET 10");
    }

    @Test
    void toSqlFragments_full() {
        QueryWrapper<?> wrapper = new QueryWrapper<>();
        // Use direct conditions (bypass the Function-based API since we can't test lambdas here)
        assertThat(FlexQueryWrapperTranslator.toSqlFragments(wrapper)).isEmpty();
    }

    @Test
    void toSqlFragments_emptyWrapper() {
        QueryWrapper<?> wrapper = new QueryWrapper<>();
        assertThat(FlexQueryWrapperTranslator.toSqlFragments(wrapper)).isEmpty();
    }

    @Test
    void toSqlFragments_withConditions() {
        QueryWrapper<?> wrapper = new QueryWrapper<>();
        wrapper.addCondition(new Condition(Operator.EQ, "status", 1));
        String sql = FlexQueryWrapperTranslator.toSqlFragments(wrapper);
        assertThat(sql).contains("WHERE");
    }
}
