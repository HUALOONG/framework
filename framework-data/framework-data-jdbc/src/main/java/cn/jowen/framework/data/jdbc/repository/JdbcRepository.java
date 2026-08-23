package cn.jowen.framework.data.jdbc.repository;

import cn.jowen.framework.data.core.repository.CrudRepository;
import cn.jowen.framework.data.core.repository.DynamicRepository;
import cn.jowen.framework.data.core.repository.PagingRepository;
import org.jspecify.annotations.NullMarked;

/**
 * JDBC 仓储接口：聚合 CRUD / 分页 / 动态 SQL 三类能力。
 *
 * @param <T>  实体类型
 * @param <ID> 主键类型
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public interface JdbcRepository<T, ID> extends CrudRepository<T, ID>, PagingRepository<T, ID>, DynamicRepository {
}
