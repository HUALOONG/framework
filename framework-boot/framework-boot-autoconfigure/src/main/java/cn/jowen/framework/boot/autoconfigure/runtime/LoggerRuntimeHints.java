package cn.jowen.framework.boot.autoconfigure.runtime;

import cn.jowen.framework.core.desensitize.DesensitizeField;
import org.jspecify.annotations.NullMarked;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM 原生镜像提示：日志模块（脱敏注解反射，扫描用户对象字段时保留元数据）。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class LoggerRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        MemberCategory[] annotations = {
                MemberCategory.INVOKE_PUBLIC_METHODS,
                MemberCategory.INVOKE_DECLARED_METHODS
        };
        hints.reflection().registerType(DesensitizeField.class, annotations);
    }
}