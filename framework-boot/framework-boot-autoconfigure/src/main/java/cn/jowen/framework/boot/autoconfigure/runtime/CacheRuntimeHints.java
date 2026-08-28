package cn.jowen.framework.boot.autoconfigure.runtime;

import cn.jowen.framework.cache.serializer.JacksonSerializer;
import cn.jowen.framework.cache.serializer.KryoSerializer;
import org.jspecify.annotations.NullMarked;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM 原生镜像提示：缓存模块（默认/可选序列化器反射实例化）。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class CacheRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        MemberCategory[] members = {
                MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS
        };
        for (Class<?> serializer : new Class<?>[]{JacksonSerializer.class, KryoSerializer.class}) {
            hints.reflection().registerType(serializer, members);
        }
    }
}