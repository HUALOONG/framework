package cn.jowen.framework.data.core.datasource;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 数据源路由器，支持按名称切换多个 {@link DataSource}。
 * 由实现层决定路由策略（动态代理、AOP、线程/ScopedValue 上下文等）。
 */
@NullMarked
public interface DataSourceRouter {

    DataSource getDataSource();

    @Nullable DataSource getDataSource(String name);

    List<DataSource> getAllDataSources();

    void setCurrentDataSource(String name);

    void clearCurrentDataSource();
}