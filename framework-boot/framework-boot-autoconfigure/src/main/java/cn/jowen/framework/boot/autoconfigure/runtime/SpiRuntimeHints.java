package cn.jowen.framework.boot.autoconfigure.runtime;

import cn.jowen.framework.core.spi.Activate;
import cn.jowen.framework.core.spi.ExtensionLoader;
import cn.jowen.framework.core.spi.ExtensionSource;
import cn.jowen.framework.core.spi.NamedExtension;
import cn.jowen.framework.core.spi.SPI;
import cn.jowen.framework.core.spi.SPIImplementation;
import cn.jowen.framework.core.spi.ServiceLoaderExtensionSource;
import cn.jowen.framework.plugin.api.Plugin;
import cn.jowen.framework.plugin.spi.PluginExtensionSource;
import cn.jowen.framework.plugin.spi.PluginSpiBridge;
import org.jspecify.annotations.NullMarked;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM 原生镜像提示：核心 SPI 与插件接入点（ServiceLoader 反射实例化）。
 *
 * <p>集中登记于 {@code boot-autoconfigure}，因 core/plugin/extras 等基础模块刻意保持 Spring 无关
 * （无 spring-core 依赖），无法各自承载 AOT registrar；沿用本模块既有 6 个 registrar 的集中模式。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
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
        // 核心扩展点：ExtensionLoader 经 ServiceLoader 反射实例化扩展实现
        hints.reflection().registerType(ExtensionLoader.class, spi);
        hints.reflection().registerType(ExtensionSource.class, spi);
        hints.reflection().registerType(NamedExtension.class, spi);
        hints.reflection().registerType(ServiceLoaderExtensionSource.class, spi);
        // 插件实例由 ServiceLoader 反射创建
        hints.reflection().registerType(Plugin.class, spi);
        hints.reflection().registerType(PluginExtensionSource.class, spi);
        hints.reflection().registerType(PluginSpiBridge.class, spi);
    }
}