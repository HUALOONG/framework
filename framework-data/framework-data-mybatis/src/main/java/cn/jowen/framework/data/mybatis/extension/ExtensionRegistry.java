package cn.jowen.framework.data.mybatis.extension;

import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@NullMarked
public final class ExtensionRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionRegistry.class);

    private final List<Extension> extensions = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Extension> extensionMap = Collections.synchronizedMap(new HashMap<>());

    public ExtensionRegistry() {}

    public static ExtensionRegistry defaults() {
        ExtensionRegistry registry = new ExtensionRegistry();
        registry.register(new FlexTenantHandler());
        registry.register(new FlexAuditHandler());
        registry.register(new FlexMaskProcessor());
        registry.register(new FlexEncryptProcessor());
        registry.register(new FlexSqlAuditListener());
        registry.register(new FlexOptimisticLockHandler());
        registry.register(new FlexLogicDeleteHandler());
        return registry;
    }

    public void register(Extension extension) {
        if (extension == null) throw new IllegalArgumentException("extension must not be null");
        synchronized (extensionMap) {
            if (extensionMap.containsKey(extension.name())) {
                logger.warn("扩展 '" + extension.name() + "' 已存在，跳过重复注册");
                return;
            }
            extensions.add(extension);
            extensionMap.put(extension.name(), extension);
            extensions.sort(Comparator.comparingInt(Extension::order));
        }
    }

    public Optional<Extension> get(String name) { return Optional.ofNullable(extensionMap.get(name)); }
    public List<Extension> getAll() { return List.copyOf(extensions); }

    public FlexTenantHandler getTenantHandler() { return (FlexTenantHandler) extensionMap.get("tenant"); }
    public FlexAuditHandler getAuditHandler() { return (FlexAuditHandler) extensionMap.get("audit"); }
    public FlexMaskProcessor getMaskProcessor() { return (FlexMaskProcessor) extensionMap.get("mask"); }
    public FlexEncryptProcessor getEncryptProcessor() { return (FlexEncryptProcessor) extensionMap.get("encrypt"); }
    public FlexSqlAuditListener getSqlAuditListener() { return (FlexSqlAuditListener) extensionMap.get("sqlAudit"); }
    public FlexOptimisticLockHandler getOptimisticLockHandler() { return (FlexOptimisticLockHandler) extensionMap.get("optimisticLock"); }
    public FlexLogicDeleteHandler getLogicDeleteHandler() { return (FlexLogicDeleteHandler) extensionMap.get("logicDelete"); }

    public void firePreSave(Object entity) {}

    public void firePostSave(Object entity) {
        FlexMaskProcessor processor = getMaskProcessor();
        if (processor != null) processor.maskObject(entity);
        FlexAuditHandler auditHandler = getAuditHandler();
        if (auditHandler != null) auditHandler.onInsert(entity);
    }

    public void firePreUpdate(Object entity) {
        FlexOptimisticLockHandler handler = getOptimisticLockHandler();
        if (handler != null) handler.incrementVersion(entity);
    }

    public void firePostUpdate(Object entity) {
        FlexMaskProcessor processor = getMaskProcessor();
        if (processor != null) processor.maskObject(entity);
        FlexAuditHandler auditHandler = getAuditHandler();
        if (auditHandler != null) auditHandler.onUpdate(entity);
    }

    public interface Extension {
        String name();
        int order();
    }
}
