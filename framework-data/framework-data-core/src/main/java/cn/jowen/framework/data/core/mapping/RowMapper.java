package cn.jowen.framework.data.core.mapping;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 行映射器：将一行（列名 → 值）映射为实体对象。零 JDBC 依赖，以 {@link Map} 承载行，使抽象层不耦合 {@code java.sql}。
 *
 * @param <T> 目标类型
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public interface RowMapper<T> {

    /**
     * 映射一行。
     *
     * @param row     列名到值的映射，不可为 {@code null}
     * @param rowNum  行号（从 0 起）
     * @return 映射后的实体，可为 {@code null}（实现决定是否跳过）
     */
    @Nullable T mapRow(Map<String, Object> row, int rowNum);
}
