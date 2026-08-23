package cn.jowen.framework.data.mybatis.adapter;

import cn.jowen.framework.data.core.datasource.DataSource;
import cn.jowen.framework.data.core.datasource.DataSourceContext;
import cn.jowen.framework.data.core.datasource.DataSourceRouter;
import cn.jowen.framework.data.core.datasource.PoolType;
import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@NullMarked
public final class FlexDataSourceAdapter implements DataSourceRouter {

    private static final Logger logger = LoggerFactory.getLogger(FlexDataSourceAdapter.class);

    private final Object dynamicDataSource;
    private final List<DataSourceEntry> entries = Collections.synchronizedList(new ArrayList<>());

    public FlexDataSourceAdapter(Object dynamicDataSource) {
        if (dynamicDataSource == null) throw new IllegalArgumentException("dynamicDataSource must not be null");
        this.dynamicDataSource = dynamicDataSource;
    }

    @Override public DataSource getDataSource() {
        for (DataSourceEntry entry : entries) { return entry.dataSource(); }
        return new DataSource() {
            @Override public String getName() { return "default"; }
            @Override public String getUrl() { return ""; }
            @Override public String getUsername() { return ""; }
            @Override public String getPassword() { return null; }
            @Override public PoolType getPoolType() { return PoolType.SIMPLE; }
        };
    }

    @Override public @Nullable DataSource getDataSource(String name) {
        for (DataSourceEntry entry : entries) {
            if (entry.name().equals(name)) return entry.dataSource();
        }
        return null;
    }

    @Override public List<DataSource> getAllDataSources() {
        List<DataSource> sources = new ArrayList<>(entries.size());
        for (DataSourceEntry entry : entries) sources.add(entry.dataSource());
        return Collections.unmodifiableList(sources);
    }

    @Override public void setCurrentDataSource(String name) {
        if (name == null) throw new IllegalArgumentException("name must not be null");
        DataSourceContext.setDataSourceKey(name);
        try {
            var method = dynamicDataSource.getClass().getMethod("determineCurrentLookupKey");
            method.invoke(dynamicDataSource);
        } catch (Exception e) {
            logger.debug("动态数据源切换: " + name);
        }
    }

    @Override public void clearCurrentDataSource() {
        DataSourceContext.clear();
    }

    public void registerDataSource(String name, DataSource dataSource) {
        if (name == null) throw new IllegalArgumentException("name must not be null");
        if (dataSource == null) throw new IllegalArgumentException("dataSource must not be null");
        entries.add(new DataSourceEntry(name, dataSource));
        logger.info("注册数据源: " + name);
    }

    private record DataSourceEntry(String name, DataSource dataSource) {}
}
