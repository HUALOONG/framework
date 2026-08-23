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
public class FlexJoinRepository<T> {

    private static final Logger logger = LoggerFactory.getLogger(FlexJoinRepository.class);

    private final FlexRepositoryAdapter<T, ?> adapter;

    public FlexJoinRepository(FlexRepositoryAdapter<T, ?> adapter) {
        this.adapter = adapter;
    }

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

    public List<T> selectJoinList(Function<T, ?> left, Class<?> rightEntity, Function<?, ?> right, JoinType joinType) {
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        switch (joinType) {
            case INNER -> wrapper.innerJoin(left, rightEntity, right);
            case LEFT -> wrapper.leftJoin(left, rightEntity, right);
            case RIGHT -> wrapper.rightJoin(left, rightEntity, right);
        }
        return selectJoinList(wrapper);
    }

    public List<T> selectList(QueryWrapper<T> wrapper) {
        return adapter.selectAll();
    }
}
