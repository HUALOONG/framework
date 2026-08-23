package cn.jowen.framework.data.core.dialect;

import cn.jowen.framework.core.spi.SPI;

/**
 * 方言检测器（SPI），负责根据连接信息自动识别数据库类型并返回对应 {@link Dialect} 实现。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@SPI
public interface DialectDetector {

    /**
     * 根据数据库产品信息检测方言。
     *
     * @param product 数据库产品名称（如 MySQL、PostgreSQL），不可为 {@code null}
     * @param version 数据库版本号，不可为 {@code null}
     * @return 对应的方言实例，不可为 {@code null}
     * @throws cn.jowen.framework.data.core.exception.DataAccessException 无法识别数据库类型时抛出
     */
    DatabaseDialect detect(String product, String version);
}
