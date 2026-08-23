package cn.jowen.framework.extras.config;

import org.jspecify.annotations.NullMarked;

/**
 * 文件存储 Bean 工厂。
 *
 * <p>负责创建和配置存储相关 Bean，由 boot-autoconfigure 调用。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public class StorageFactory {

    private final StorageProperties properties;

    public StorageFactory(StorageProperties properties) {
        this.properties = properties;
    }

    // TODO: 实现存储 Bean 工厂方法
}
