package cn.jowen.framework.data.core.datasource;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认数据源路由器：按名称维护多数据源，路由键取自 {@link DataSourceContext}；
 * 未显式切换或键名下不存在数据源时回退默认源。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class DefaultDataSourceRouter implements DataSourceRouter {

    private final Map<String, DataSource> sources = new ConcurrentHashMap<>();
    private final DataSource defaultSource;

    /**
     * 构造路由器，首个注册项即默认源。
     *
     * @param defaultSource 默认数据源，不可为 {@code null}
     */
    public DefaultDataSourceRouter(DataSource defaultSource) {
        if (defaultSource == null) {
            throw new IllegalArgumentException("defaultSource cannot be null");
        }
        this.defaultSource = defaultSource;
        this.sources.put(defaultSource.getName(), defaultSource);
    }

    /**
     * 注册（或覆盖）指定名称的数据源。
     *
     * @param name   数据源名称，不可为空
     * @param source 数据源，不可为 {@code null}
     */
    public void register(String name, DataSource source) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("数据源名称不能为空");
        }
        if (source == null) {
            throw new IllegalArgumentException("数据源不能为 null");
        }
        sources.put(name, source);
    }

    @Override
    public DataSource getDataSource() {
        String key = DataSourceContext.getDataSourceKey();
        if (key != null) {
            DataSource current = sources.get(key);
            if (current != null) {
                return current;
            }
        }
        return defaultSource;
    }

    @Override
    public @Nullable DataSource getDataSource(String name) {
        return sources.get(name);
    }

    @Override
    public List<DataSource> getAllDataSources() {
        return List.copyOf(sources.values());
    }

    @Override
    public void setCurrentDataSource(String name) {
        if (getDataSource(name) == null) {
            throw new IllegalArgumentException("未知数据源：" + name);
        }
        DataSourceContext.setDataSourceKey(name);
    }

    @Override
    public void clearCurrentDataSource() {
        DataSourceContext.clear();
    }
}