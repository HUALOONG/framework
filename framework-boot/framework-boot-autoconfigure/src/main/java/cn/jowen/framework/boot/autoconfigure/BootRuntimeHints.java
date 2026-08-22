package cn.jowen.framework.boot.autoconfigure;

import cn.jowen.framework.core.spi.Activate;
import cn.jowen.framework.core.spi.SPI;
import cn.jowen.framework.core.spi.SPIImplementation;
import cn.jowen.framework.data.core.meta.Column;
import cn.jowen.framework.data.core.meta.GeneratedValue;
import cn.jowen.framework.data.core.meta.Id;
import cn.jowen.framework.data.core.meta.Table;
import cn.jowen.framework.data.jdbc.mapping.BeanRowMapper;
import cn.jowen.framework.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * 全模块 GraalVM AOT 运行时提示（RuntimeHints），供原生镜像构建反射/资源注册使用。
 *
 * <p>经 {@code META-INF/spring/aot.factories} 注册，覆盖：数据元注解（字段反射）、
 * 插件与核心 SPI（ServiceLoader 实例化）、JDBC 行映射器（构造反射）、i18n 资源包。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public class BootRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        registerDataHints(hints);
        registerSpiHints(hints);
        registerResourceHints(hints);
    }

    /** 数据模块：元注解与行映射器的反射需求。 */
    private void registerDataHints(RuntimeHints hints) {
        MemberCategory[] annotations = {
                MemberCategory.INVOKE_PUBLIC_METHODS,
                MemberCategory.INVOKE_DECLARED_METHODS
        };
        hints.reflection().registerType(Table.class, annotations);
        hints.reflection().registerType(Id.class, annotations);
        hints.reflection().registerType(Column.class, annotations);
        hints.reflection().registerType(GeneratedValue.class, annotations);
        // BeanRowMapper 通过反射实例化用户实体并写字段
        hints.reflection().registerType(BeanRowMapper.class, MemberCategory.INVOKE_PUBLIC_METHODS);
    }

    /** 核心与插件 SPI：ServiceLoader 反射实例化。 */
    private void registerSpiHints(RuntimeHints hints) {
        MemberCategory[] spi = {
                MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS
        };
        hints.reflection().registerType(SPI.class, spi);
        hints.reflection().registerType(SPIImplementation.class, spi);
        hints.reflection().registerType(Activate.class, spi);
        hints.reflection().registerType(Plugin.class, spi);
    }

    /** 资源模块：i18n 资源包纳入原生镜像。 */
    private void registerResourceHints(RuntimeHints hints) {
        hints.resources().registerPattern("i18n/*.properties");
    }
}
