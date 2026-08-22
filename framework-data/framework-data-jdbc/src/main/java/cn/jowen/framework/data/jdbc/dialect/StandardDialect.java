package cn.jowen.framework.data.jdbc.dialect;

import cn.jowen.framework.data.core.dialect.Dialect;
import cn.jowen.framework.data.core.page.Pageable;
import org.jspecify.annotations.NullMarked;

/**
 * 通用方言（H2 / MySQL / PostgreSQL 兼容的 LIMIT/OFFSET 分页）。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class StandardDialect implements Dialect {

    @Override
    public String buildPageSql(String sql, Pageable pageable) {
        StringBuilder sb = new StringBuilder(sql);
        if (!pageable.getSort().isEmpty()) {
            sb.append(" ORDER BY ");
            boolean first = true;
            for (cn.jowen.framework.data.core.page.Sort.Order order : pageable.getSort().getOrders()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(order.toSql());
                first = false;
            }
        }
        sb.append(" LIMIT ").append(pageable.getSize()).append(" OFFSET ").append(pageable.getOffset());
        return sb.toString();
    }

    @Override
    public String name() {
        return "standard";
    }
}
