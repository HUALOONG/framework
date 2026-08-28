package cn.jowen.framework.data.mybatis.query;

import cn.jowen.framework.data.core.query.Condition;
import cn.jowen.framework.data.core.query.Operator;
import cn.jowen.framework.data.core.query.QueryWrapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 覆盖 {@link FlexLambdaQueryBuilder} 的全部链式构造方法（纯逻辑，无运行期依赖）。
 *
 * @author software-engineer-2
 */
class FlexLambdaQueryBuilderTest {

    /** 测试用实体（仅承载字段与 getter，运行期不会被实例化）。 */
    static final class Sample {
        private String name;
        private Integer age;
        private Long deptId;

        public String getName() {
            return name;
        }

        public Integer getAge() {
            return age;
        }

        public Long getDeptId() {
            return deptId;
        }
    }

    /** 关联实体。 */
    static final class Other {
        private Long id;

        public Long getId() {
            return id;
        }
    }

    /**
     * 可解析列：让 {@link QueryWrapper} 的列名解析走确定路径，
     * 避免依赖 Lambda/方法引用的 {@code toString()} 实现。
     */
    static final class Col implements QueryWrapper.ResolvableFunction<Sample, Object> {
        private final String name;

        Col(String name) {
            this.name = name;
        }

        @Override
        public String getColumnName() {
            return name;
        }

        @Override
        public Object apply(Sample sample) {
            return null;
        }
    }

    static final class OtherCol implements QueryWrapper.ResolvableFunction<Other, Object> {
        private final String name;

        OtherCol(String name) {
            this.name = name;
        }

        @Override
        public String getColumnName() {
            return name;
        }

        @Override
        public Object apply(Other other) {
            return null;
        }
    }

    @Test
    void create_returnsBuilderWithWrapper() {
        FlexLambdaQueryBuilder<Sample> builder = FlexLambdaQueryBuilder.create(Sample.class);
        assertThat(builder).isNotNull();
        assertThat(builder.getWrapper()).isNotNull();
    }

    @Test
    void comparisonMethods_buildExpectedConditions() {
        FlexLambdaQueryBuilder<Sample> builder = FlexLambdaQueryBuilder.create(Sample.class);
        builder.eq(new Col("name"), "Alice")
                .ne(new Col("id"), 1)
                .gt(new Col("age"), 18)
                .gte(new Col("age"), 18)
                .lt(new Col("age"), 60)
                .lte(new Col("age"), 60)
                .like(new Col("name"), "Ali")
                .in(new Col("id"), List.of(1, 2, 3))
                .between(new Col("age"), 18, 60)
                .isNull(new Col("name"))
                .isNotNull(new Col("name"));

        List<Condition> conditions = builder.getWrapper().getConditions();
        assertThat(conditions).hasSize(11);

        assertThat(conditions.get(0).getOperator()).isEqualTo(Operator.EQ);
        assertThat(conditions.get(0).getColumn()).isEqualTo("name");
        assertThat(conditions.get(0).getValue()).isEqualTo("Alice");

        assertThat(conditions.get(1).getOperator()).isEqualTo(Operator.NE);
        assertThat(conditions.get(2).getOperator()).isEqualTo(Operator.GT);
        assertThat(conditions.get(3).getOperator()).isEqualTo(Operator.GTE);
        assertThat(conditions.get(4).getOperator()).isEqualTo(Operator.LT);
        assertThat(conditions.get(5).getOperator()).isEqualTo(Operator.LTE);
        assertThat(conditions.get(6).getOperator()).isEqualTo(Operator.LIKE);
        assertThat(conditions.get(7).getOperator()).isEqualTo(Operator.IN);
        assertThat(conditions.get(7).getValue()).isEqualTo(List.of(1, 2, 3));
        assertThat(conditions.get(8).getOperator()).isEqualTo(Operator.BETWEEN);
        assertThat(conditions.get(9).getOperator()).isEqualTo(Operator.IS_NULL);
        assertThat(conditions.get(10).getOperator()).isEqualTo(Operator.IS_NOT_NULL);
    }

    @Test
    void orderByAndPaging_populateWrapper() {
        FlexLambdaQueryBuilder<Sample> builder = FlexLambdaQueryBuilder.create(Sample.class);
        builder.orderByAsc(new Col("name")).orderByDesc(new Col("age")).limit(10).offset(5);

        QueryWrapper<Sample> wrapper = builder.getWrapper();
        assertThat(wrapper.getOrderBy()).containsExactly("name ASC", "age DESC");
        assertThat(wrapper.getLimit()).isEqualTo(10);
        assertThat(wrapper.getOffset()).isEqualTo(5);
    }

    @Test
    void joins_registerJoinClauses() {
        FlexLambdaQueryBuilder<Sample> builder = FlexLambdaQueryBuilder.create(Sample.class);
        builder.innerJoin(new Col("dept_id"), Other.class, new OtherCol("id"));
        builder.leftJoin(new Col("dept_id"), Other.class, new OtherCol("id"));

        List<QueryWrapper.Join<Sample>> joins = builder.getWrapper().getJoins();
        assertThat(joins).hasSize(2);
        assertThat(joins.get(0).rightEntity()).isEqualTo(Other.class);
        assertThat(joins.get(1).rightEntity()).isEqualTo(Other.class);
    }

    @Test
    void fluentMethods_returnSameBuilder() {
        FlexLambdaQueryBuilder<Sample> builder = FlexLambdaQueryBuilder.create(Sample.class);
        assertThat(builder.eq(new Col("name"), "x")).isSameAs(builder);
        assertThat(builder.limit(1)).isSameAs(builder);
    }
}
