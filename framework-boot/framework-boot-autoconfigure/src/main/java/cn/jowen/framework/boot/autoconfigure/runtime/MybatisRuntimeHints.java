package cn.jowen.framework.boot.autoconfigure.runtime;

import cn.jowen.framework.data.mybatis.extension.ExtensionRegistry;
import cn.jowen.framework.data.mybatis.extension.FlexEncryptProcessor;
import cn.jowen.framework.data.mybatis.extension.FlexLogicDeleteHandler;
import cn.jowen.framework.data.mybatis.extension.FlexMaskProcessor;
import cn.jowen.framework.data.mybatis.extension.FlexOptimisticLockHandler;
import cn.jowen.framework.data.mybatis.extension.FlexSqlAuditListener;
import cn.jowen.framework.data.mybatis.extension.FlexTenantHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM 原生镜像提示：MyBatis Flex 扩展（注册中心与各扩展点反射实例化）。
 *
 * @author 王飞
 * @since 2026-08-27
 */
@NullMarked
public class MybatisRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        MemberCategory[] members = {
                MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS
        };
        for (Class<?> type : new Class<?>[]{
                ExtensionRegistry.class,
                FlexMaskProcessor.class,
                FlexEncryptProcessor.class,
                FlexTenantHandler.class,
                FlexLogicDeleteHandler.class,
                FlexSqlAuditListener.class,
                FlexOptimisticLockHandler.class}) {
            hints.reflection().registerType(type, members);
        }
    }
}