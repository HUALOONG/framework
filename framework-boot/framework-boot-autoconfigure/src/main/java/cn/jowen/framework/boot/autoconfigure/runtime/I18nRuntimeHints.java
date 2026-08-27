package cn.jowen.framework.boot.autoconfigure.runtime;

import org.jspecify.annotations.NullMarked;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM 原生镜像提示：国际化模块（消息资源文件纳入镜像）。
 *
 * @author 王飞
 * @since 2026-08-27
 */
@NullMarked
public class I18nRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerPattern("messages*.properties");
        hints.resources().registerPattern("i18n/**/*.properties");
    }
}