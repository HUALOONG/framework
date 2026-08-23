package cn.jowen.framework.plugin.loader;

import org.jspecify.annotations.NullMarked;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * 共享类加载器：插件间共享第三方库（Jackson/Guava），避免版本冲突。
 *
 * <p>所有插件 ClassLoader 可将此类加载器作为 parent 之一，从而共享同一份第三方库版本。
 * 宿主配置 {@code framework.plugin.class-loading.shared-libraries} 指定需要共享的库坐标，
 * 由装配层将对应 URL 注入此加载器。
 *
 * @author 王飞
 */
@NullMarked
public final class SharedClassLoader extends URLClassLoader {

    /**
     * 构造共享类加载器。
     *
     * @param urls   共享库 URL 列表，可为空
     * @param parent 父加载器（通常为应用加载器）
     */
    public SharedClassLoader(URL[] urls, ClassLoader parent) {
        super(urls == null ? new URL[0] : urls, parent);
    }
}
