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
/**
 * 「FlexDataSourceAdapter」封装相关能力。
 *
 * @author Jowen
 * @since 0.0.1
 * @version 0.0.1
 */
public final class FlexDataSourceAdapter implements DataSourceRouter {

    /** logger 常量。 */
    private static final Logger logger = LoggerFactory.getLogger(FlexDataSourceAdapter.class);

    /** dynamicDataSource 不可变字段。 */
    private final Object dynamicDataSource;
    /** entries 不可变字段。 */
    private final List<DataSourceEntry> entries = Collections.synchronizedList(new ArrayList<>());

    /**
     * 构造实例。
     * @param dynamicDataSource 参数 dynamicDataSource
     */
    public FlexDataSourceAdapter(Object dynamicDataSource) {
        if (dynamicDataSource == null) throw new IllegalArgumentException("dynamicDataSource must not be null");
        this.dynamicDataSource = dynamicDataSource;
    }

    /**
     * 获取data source。
     * @return 结果
     */
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

    /**
     * 获取data source。
     * @param name 参数 name
     * @return 结果
     */
    @Override public @Nullable DataSource getDataSource(String name) {
        for (DataSourceEntry entry : entries) {
            if (entry.name().equals(name)) return entry.dataSource();
        }
        return null;
    }

    /**
     * 获取all data sources。
     * @return 结果
     */
    @Override public List<DataSource> getAllDataSources() {
        List<DataSource> sources = new ArrayList<>(entries.size());
        for (DataSourceEntry entry : entries) sources.add(entry.dataSource());
        return Collections.unmodifiableList(sources);
    }

    /**
     * 设置current data source。
     * @param name 参数 name
     */
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

    /**
     * 设置ar current data source。
     */
    @Override public void clearCurrentDataSource() {
        DataSourceContext.clear();
    }

    /**
     * 设置ister data source。
     * @param name 参数 name
     * @param dataSource 参数 dataSource
     */
    public void registerDataSource(String name, DataSource dataSource) {
        if (name == null) throw new IllegalArgumentException("name must not be null");
        if (dataSource == null) throw new IllegalArgumentException("dataSource must not be null");
        entries.add(new DataSourceEntry(name, dataSource));
        logger.info("注册数据源: " + name);
    }

    /**
     * 「DataSourceEntry」不可变数据载体。
     *
     * @author Jowen
     * @since 0.0.1
 * @version 0.0.1
     */
    private record DataSourceEntry(String name, DataSource dataSource) {}
}
