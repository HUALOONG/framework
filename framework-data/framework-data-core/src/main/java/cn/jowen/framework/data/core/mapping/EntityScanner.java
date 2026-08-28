package cn.jowen.framework.data.core.mapping;

import cn.jowen.framework.core.spi.SPI;
import org.jspecify.annotations.NullMarked;

import java.util.Collection;

/**
 * 实体扫描器（SPI），负责发现并返回所有可持久化的实体类。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@SPI
@NullMarked
public interface EntityScanner {

    /**
     * 扫描指定包路径下的所有实体类。
     *
     * @param basePackages 基础包列表，不可为 {@code null}
     * @return 发现的实体类集合，不可为 {@code null}
     */
    Collection<Class<?>> scan(String... basePackages);
}
