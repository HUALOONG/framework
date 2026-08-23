package cn.jowen.framework.extras.config;

import org.jspecify.annotations.NullMarked;

/**
 * Excel Bean 工厂。
 *
 * <p>负责创建和配置 Excel 处理相关 Bean，由 boot-autoconfigure 调用。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public class ExcelFactory {

    private final ExcelProperties properties;

    public ExcelFactory(ExcelProperties properties) {
        this.properties = properties;
    }

    // TODO: 实现 Excel Bean 工厂方法
}
