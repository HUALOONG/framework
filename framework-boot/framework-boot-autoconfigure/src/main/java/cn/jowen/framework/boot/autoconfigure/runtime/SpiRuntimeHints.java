package cn.jowen.framework.boot.autoconfigure.runtime;

import cn.jowen.framework.core.spi.Activate;
import cn.jowen.framework.core.spi.SPI;
import cn.jowen.framework.core.spi.SPIImplementation;
import cn.jowen.framework.plugin.api.Plugin;
import org.jspecify.annotations.NullMarked;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM 原生镜像提示：核心 SPI 与插件接入点（ServiceLoader 反射实例化）。
 *
 * @author 王飞
 * @since 2026-08-27
 */
@NullMarked
public class SpiRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        MemberCategory[] spi = {
                MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS
        };
        hints.reflection().registerType(SPI.class, spi);
        hints.reflection().registerType(SPIImplementation.class, spi);
        hints.reflection().registerType(Activate.class, spi);
        // 插件实例由 ServiceLoader 反射创建
        hints.reflection().registerType(Plugin.class, spi);
    }
}