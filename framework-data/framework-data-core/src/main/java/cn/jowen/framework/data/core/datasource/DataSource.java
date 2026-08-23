package cn.jowen.framework.data.core.datasource;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 数据源抽象描述。仅承载连接元信息，不持有物理连接；实现层据此创建真实 {@code javax.sql.DataSource}。
 */
@NullMarked
public interface DataSource {

    String getName();

    String getUrl();

    String getUsername();

    @Nullable String getPassword();

    PoolType getPoolType();

    default boolean isInitialized() {
        return getUrl() != null && !getUrl().isBlank();
    }

    default Map<String, String> getProperties() {
        return Map.of();
    }
}