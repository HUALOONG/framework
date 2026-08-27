package cn.jowen.framework.core.spi;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * ServiceLoader 静态发现源：{@link ExtensionLoader} 的默认源，经 META-INF/services 发现实现。
 *
 * <p>候选派生规则：标注 {@link SPIImplementation} 的实现以 {@code name()} 作为实现名；仅标注
 * {@link Activate} 的实现默认使用类简单名；两者皆无的实现不参与加载（与原 ExtensionLoader 行为一致）。
 * 标注 {@link Activate} 的实现 {@code order} 取 {@code @Activate.order()}，优先级高于
 * {@code @SPIImplementation.order()}（与原激活排序语义一致）。
 *
 * @param <T> 扩展点接口类型
 * @author 王飞
 * @since 2026-08-27
 */
public final class ServiceLoaderExtensionSource<T> implements ExtensionSource<T> {

    public static final String SOURCE_ID = "serviceloader";

    @Override
    public String sourceId() {
        return SOURCE_ID;
    }

    @Override
    public List<NamedExtension<T>> load(Class<T> extensionPoint, ClassLoader classLoader) {
        List<NamedExtension<T>> result = new ArrayList<>();
        for (T candidate : ServiceLoader.load(extensionPoint, classLoader)) {
            SPIImplementation impl = candidate.getClass().getAnnotation(SPIImplementation.class);
            Activate activate = candidate.getClass().getAnnotation(Activate.class);
            if (impl == null && activate == null) {
                continue;
            }
            // 原语义：getExtension 仅认 @SPIImplementation；getActivateExtensions 认 @Activate
            if (impl != null) {
                result.add(create(impl.name(), impl.order(), activate, candidate));
            } else {
                result.add(create(candidate.getClass().getSimpleName(), 0, activate, candidate));
            }
        }
        return result;
    }

    private static <T> NamedExtension<T> create(String name, int implOrder,
                                                @Nullable Activate activate, T instance) {
        if (activate != null) {
            return new NamedExtension<>(name, instance, activate.order(), activate.group(),
                    true, SOURCE_ID);
        }
        return new NamedExtension<>(name, instance, implOrder, new String[0], false, SOURCE_ID);
    }
}