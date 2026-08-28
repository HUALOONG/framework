package cn.jowen.framework.data.mybatis.repository;

import cn.jowen.framework.data.core.query.JoinType;
import cn.jowen.framework.data.core.query.QueryWrapper;
import cn.jowen.framework.data.mybatis.adapter.FlexRepositoryAdapter;
import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.function.Function;

@NullMarked
/**
 * 「FlexJoinRepository」封装相关能力。
 *
 * @author Jowen
 * @since 0.0.1
 * @version 0.0.1
 */
public class FlexJoinRepository<T> {

    /** logger 常量。 */
    private static final Logger logger = LoggerFactory.getLogger(FlexJoinRepository.class);

    /** adapter 不可变字段。 */
    private final FlexRepositoryAdapter<T, ?> adapter;

    /**
     * 构造实例。
     */
    public FlexJoinRepository(FlexRepositoryAdapter<T, ?> adapter) {
        this.adapter = adapter;
    }

    /**
     * 执行@ suppress warnings操作。
     * @return 结果
     */
    @SuppressWarnings("unchecked")
    public List<T> selectJoinList(QueryWrapper<T> wrapper) {
        if (wrapper.getJoins().isEmpty()) return adapter.selectAll();
        logger.debug("执行 join 查询: joins=" + wrapper.getJoins().size());
        try {
            var method = adapter.getBaseMapper().getClass().getMethod("selectList", QueryWrapper.class);
            return (List<T>) method.invoke(adapter.getBaseMapper(), wrapper);
        } catch (Exception e) {
            logger.warn("join 查询反射失败，回退为 selectAll: " + e.getMessage());
            return adapter.selectAll();
        }
    }

    /**
     * 执行select join list操作。
     * @param joinType 参数 joinType
     * @return 结果
     */
    public List<T> selectJoinList(Function<T, ?> left, Class<?> rightEntity, Function<?, ?> right, JoinType joinType) {
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        switch (joinType) {
            case INNER -> wrapper.innerJoin(left, rightEntity, right);
            case LEFT -> wrapper.leftJoin(left, rightEntity, right);
            case RIGHT -> wrapper.rightJoin(left, rightEntity, right);
        }
        return selectJoinList(wrapper);
    }

    /**
     * 执行select list操作。
     * @return 结果
     */
    public List<T> selectList(QueryWrapper<T> wrapper) {
        return adapter.selectAll();
    }
}
