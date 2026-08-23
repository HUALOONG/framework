package cn.jowen.framework.data.mybatis.repository;

import cn.jowen.framework.data.core.repository.DynamicRepository;
import cn.jowen.framework.data.mybatis.adapter.FlexExceptionTranslator;
import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;

@NullMarked
public class FlexDynamicRepository implements DynamicRepository {

    private static final Logger logger = LoggerFactory.getLogger(FlexDynamicRepository.class);

    private final Object sqlSessionFactory;

    public FlexDynamicRepository(Object sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    @Override
    public List<Map<String, Object>> query(String sql, Object... params) {
        try {
            var method = sqlSessionFactory.getClass().getMethod("selectList", String.class, Object[].class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> result = (List<Map<String, Object>>) method.invoke(sqlSessionFactory, sql, params);
            return result != null ? result : List.of();
        } catch (Exception e) {
            throw FlexExceptionTranslator.translate("dynamicQuery", null, e);
        }
    }

    @Override
    public int update(String sql, Object... params) {
        try {
            var method = sqlSessionFactory.getClass().getMethod("update", String.class, Object[].class);
            return (Integer) method.invoke(sqlSessionFactory, sql, params);
        } catch (Exception e) {
            throw FlexExceptionTranslator.translate("dynamicUpdate", null, e);
        }
    }

    @Override
    public Map<String, Object> queryOne(String sql, Object... params) {
        List<Map<String, Object>> rows = query(sql, params);
        return rows.isEmpty() ? Map.of() : rows.getFirst();
    }
}
