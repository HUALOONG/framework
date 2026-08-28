package cn.jowen.framework.data.core.repository;

import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;

/**
 * 动态 SQL 仓储接口，适用于无实体类的动态 SQL 场景。
 * 通过原始 SQL + 参数方式执行查询与更新，结果以 {@code Map<String, Object>} 呈现。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface DynamicRepository {

    /**
     * 执行查询 SQL，返回多行结果列表。
     *
     * @param sql    SQL 语句，不可为 {@code null}
     * @param params SQL 参数，按顺序绑定
     * @return 查询结果列表，每行是一个 {@code Map<String, Object>}，key 为列名
     */
    List<Map<String, Object>> query(String sql, Object... params);

    /**
     * 执行更新/删除 SQL，返回受影响行数。
     *
     * @param sql    SQL 语句，不可为 {@code null}
     * @param params SQL 参数，按顺序绑定
     * @return 受影响行数
     */
    int update(String sql, Object... params);

    /**
     * 执行查询 SQL，返回单行结果。
     *
     * @param sql    SQL 语句，不可为 {@code null}
     * @param params SQL 参数，按顺序绑定
     * @return 查询结果行，未找到时返回 {@code null}
     */
    Map<String, Object> queryOne(String sql, Object... params);
}
