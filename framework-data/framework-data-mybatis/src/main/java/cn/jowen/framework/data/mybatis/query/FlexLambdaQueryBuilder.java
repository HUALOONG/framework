package cn.jowen.framework.data.mybatis.query;

import cn.jowen.framework.data.core.query.Operator;
import cn.jowen.framework.data.core.query.QueryWrapper;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * Lambda 类型安全查询入口（静态工厂）。
 * <p>
 * 业务方直接使用 {@code FlexLambdaQueryBuilder.create(User.class)} 构建查询，
 * 绕过 {@link FlexQueryWrapperTranslator} 翻译器，降低开销。
 * </p>
 *
 * @param <T> 实体类型
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class FlexLambdaQueryBuilder<T> {

    private final QueryWrapper<T> wrapper;

    private FlexLambdaQueryBuilder(Class<T> entityClass) {
        this.wrapper = new QueryWrapper<>();
    }

    /**
     * 创建查询构建器。
     *
     * @param entityClass 实体类
     * @param <T>         实体类型
     * @return 查询构建器
     */
    public static <T> FlexLambdaQueryBuilder<T> create(Class<T> entityClass) {
        return new FlexLambdaQueryBuilder<>(entityClass);
    }

    public FlexLambdaQueryBuilder<T> eq(Function<T, ?> column, @Nullable Object value) {
        wrapper.eq(column, value);
        return this;
    }

    public FlexLambdaQueryBuilder<T> ne(Function<T, ?> column, @Nullable Object value) {
        wrapper.ne(column, value);
        return this;
    }

    public FlexLambdaQueryBuilder<T> gt(Function<T, ?> column, @Nullable Object value) {
        wrapper.gt(column, value);
        return this;
    }

    public FlexLambdaQueryBuilder<T> gte(Function<T, ?> column, @Nullable Object value) {
        wrapper.gte(column, value);
        return this;
    }

    public FlexLambdaQueryBuilder<T> lt(Function<T, ?> column, @Nullable Object value) {
        wrapper.lt(column, value);
        return this;
    }

    public FlexLambdaQueryBuilder<T> lte(Function<T, ?> column, @Nullable Object value) {
        wrapper.lte(column, value);
        return this;
    }

    public FlexLambdaQueryBuilder<T> like(Function<T, ?> column, @Nullable Object value) {
        wrapper.like(column, value);
        return this;
    }

    public FlexLambdaQueryBuilder<T> in(Function<T, ?> column, @Nullable Object values) {
        wrapper.in(column, values);
        return this;
    }

    public FlexLambdaQueryBuilder<T> between(Function<T, ?> column, Object start, Object end) {
        wrapper.between(column, start, end);
        return this;
    }

    public FlexLambdaQueryBuilder<T> isNull(Function<T, ?> column) {
        wrapper.isNull(column);
        return this;
    }

    public FlexLambdaQueryBuilder<T> isNotNull(Function<T, ?> column) {
        wrapper.isNotNull(column);
        return this;
    }

    public FlexLambdaQueryBuilder<T> orderByAsc(Function<T, ?> column) {
        wrapper.orderByAsc(column);
        return this;
    }

    public FlexLambdaQueryBuilder<T> orderByDesc(Function<T, ?> column) {
        wrapper.orderByDesc(column);
        return this;
    }

    public FlexLambdaQueryBuilder<T> limit(int limit) {
        wrapper.limit(limit);
        return this;
    }

    public FlexLambdaQueryBuilder<T> offset(int offset) {
        wrapper.offset(offset);
        return this;
    }

    public FlexLambdaQueryBuilder<T> innerJoin(Function<T, ?> left, Class<?> rightEntity, Function<?, ?> right) {
        wrapper.innerJoin(left, rightEntity, right);
        return this;
    }

    public FlexLambdaQueryBuilder<T> leftJoin(Function<T, ?> left, Class<?> rightEntity, Function<?, ?> right) {
        wrapper.leftJoin(left, rightEntity, right);
        return this;
    }

    /**
     * 获取底层 {@link QueryWrapper}。
     *
     * @return 查询包装器
     */
    public QueryWrapper<T> getWrapper() {
        return wrapper;
    }
}
