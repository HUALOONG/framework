package cn.jowen.framework.data.mybatis.repository;

import cn.jowen.framework.data.core.repository.DynamicRepository;
import cn.jowen.framework.data.mybatis.adapter.FlexExceptionTranslator;
import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;

@NullMarked
/**
 * 「FlexDynamicRepository」封装相关能力。
 *
 * @author Jowen
 * @since 0.0.1
 * @version 0.0.1
 */
public class FlexDynamicRepository implements DynamicRepository {

    /** logger 常量。 */
    private static final Logger logger = LoggerFactory.getLogger(FlexDynamicRepository.class);

    /** sqlSessionFactory 不可变字段。 */
    private final Object sqlSessionFactory;

    /**
     * 构造实例。
     * @param sqlSessionFactory 参数 sqlSessionFactory
     */
    public FlexDynamicRepository(Object sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    /**
     * 执行query操作。
     * @param sql 参数 sql
     * @param params 参数 params
     * @return 结果
     */
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

    /**
     * 执行update操作。
     * @param sql 参数 sql
     * @param params 参数 params
     * @return 结果
     */
    @Override
    public int update(String sql, Object... params) {
        try {
            var method = sqlSessionFactory.getClass().getMethod("update", String.class, Object[].class);
            return (Integer) method.invoke(sqlSessionFactory, sql, params);
        } catch (Exception e) {
            throw FlexExceptionTranslator.translate("dynamicUpdate", null, e);
        }
    }

    /**
     * 执行query one操作。
     * @param sql 参数 sql
     * @param params 参数 params
     * @return 结果
     */
    @Override
    public Map<String, Object> queryOne(String sql, Object... params) {
        List<Map<String, Object>> rows = query(sql, params);
        return rows.isEmpty() ? Map.of() : rows.getFirst();
    }
}
