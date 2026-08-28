package cn.jowen.framework.logger.adapter;

import cn.jowen.framework.core.spi.Activate;
import cn.jowen.framework.core.spi.SPI;
import cn.jowen.framework.logger.facade.Logger;
import org.jspecify.annotations.NullMarked;

/**
 * 日志适配器接口，将门面 {@link Logger} 桥接到具体日志实现（Logback/Log4j2）。
 *
 * <p>实现类标注 {@link Activate} 后被 {@link cn.jowen.framework.logger.facade.LoggerFactory} 自动发现。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@SPI
public interface LoggerAdapter {

    /**
     * 按名称创建日志器。
     *
     * @param name 名称，不可为 {@code null}
     * @return 门面日志器，不可为 {@code null}
     */
    Logger getLogger(String name);
}
