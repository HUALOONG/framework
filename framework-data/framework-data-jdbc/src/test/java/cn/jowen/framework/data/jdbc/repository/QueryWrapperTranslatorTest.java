package cn.jowen.framework.data.jdbc.repository;

import cn.jowen.framework.data.core.query.QueryWrapper;
import cn.jowen.framework.data.jdbc.statement.SqlResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link QueryWrapperTranslator} 测试：GROUP BY 拼接（含 WHERE/ORDER BY 组合）。
 */
class QueryWrapperTranslatorTest {

    private final QueryWrapperTranslator translator = new QueryWrapperTranslator();

    @Test
    void groupBy_appended() {
        QueryWrapper<Object> wrapper = new QueryWrapper<>();
        wrapper.groupBy(col("dept"));
        SqlResult result = translator.translate(wrapper, "t_user");
        assertThat(result.sql()).isEqualTo("SELECT * FROM t_user GROUP BY dept");
        assertThat(result.params()).isEmpty();
    }

    @Test
    void groupByWithWhereAndOrder() {
        QueryWrapper<Object> wrapper = new QueryWrapper<>();
        wrapper.eq(col("status"), 1);
        wrapper.groupBy(col("dept"));
        wrapper.orderByDesc(col("create_time"));
        SqlResult result = translator.translate(wrapper, "t_user");
        assertThat(result.sql())
                .isEqualTo("SELECT * FROM t_user WHERE status = ? GROUP BY dept ORDER BY create_time DESC");
        assertThat(result.params()).containsExactly(1);
    }

    @Test
    void groupByMultipleColumns() {
        QueryWrapper<Object> wrapper = new QueryWrapper<>();
        wrapper.groupBy(col("dept"), col("status"));
        SqlResult result = translator.translate(wrapper, "t_user");
        assertThat(result.sql()).isEqualTo("SELECT * FROM t_user GROUP BY dept, status");
    }

    private static QueryWrapper.ResolvableFunction<Object, Object> col(String name) {
        return new QueryWrapper.ResolvableFunction<>() {
            @Override
            public Object apply(Object o) {
                return null;
            }

            @Override
            public String getColumnName() {
                return name;
            }
        };
    }
}