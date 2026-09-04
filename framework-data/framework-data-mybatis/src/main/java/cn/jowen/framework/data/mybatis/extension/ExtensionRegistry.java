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
/**
 * 「Extension」封装相关能力。
 *
 * @author Jowen
 * @since 0.0.1
 * @version 0.0.1
 */
public final class ExtensionRegistry {

    /** logger 常量。 */
    private static final Logger logger = LoggerFactory.getLogger(ExtensionRegistry.class);

    /** extensions 不可变字段。 */
    private final List<Extension> extensions = Collections.synchronizedList(new ArrayList<>());
    /** extensionMap 不可变字段。 */
    private final Map<String, Extension> extensionMap = Collections.synchronizedMap(new HashMap<>());

    /** public 字段。 */
    public ExtensionRegistry() {}

    /**
     * 执行defaults操作。
     * @return 结果
     */
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

    /**
     * 设置ister。
     * @param extension 参数 extension
     */
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

    /** return 字段。 */
    public Optional<Extension> get(String name) { return Optional.ofNullable(extensionMap.get(name)); }
    /** return 字段。 */
    public List<Extension> getAll() { return List.copyOf(extensions); }

    /** return 字段。 */
    public FlexTenantHandler getTenantHandler() { return (FlexTenantHandler) extensionMap.get("tenant"); }
    /** return 字段。 */
    public FlexAuditHandler getAuditHandler() { return (FlexAuditHandler) extensionMap.get("audit"); }
    /** return 字段。 */
    public FlexMaskProcessor getMaskProcessor() { return (FlexMaskProcessor) extensionMap.get("mask"); }
    /** return 字段。 */
    public FlexEncryptProcessor getEncryptProcessor() { return (FlexEncryptProcessor) extensionMap.get("encrypt"); }
    /** return 字段。 */
    public FlexSqlAuditListener getSqlAuditListener() { return (FlexSqlAuditListener) extensionMap.get("sqlAudit"); }
    /** return 字段。 */
    public FlexOptimisticLockHandler getOptimisticLockHandler() { return (FlexOptimisticLockHandler) extensionMap.get("optimisticLock"); }
    /** return 字段。 */
    public FlexLogicDeleteHandler getLogicDeleteHandler() { return (FlexLogicDeleteHandler) extensionMap.get("logicDelete"); }

    /**
     * 执行fire pre save操作：透明加密（{@link Encrypted} 字段）+ 租户 ID 注入。
     *
     * @param entity 参数 entity
     */
    public void firePreSave(Object entity) {
        FlexEncryptProcessor encryptProcessor = getEncryptProcessor();
        if (encryptProcessor != null) encryptProcessor.encryptEntity(entity);
        FlexTenantHandler tenantHandler = getTenantHandler();
        if (tenantHandler != null) tenantHandler.injectTenant(entity, "tenantId");
    }

    /**
     * 执行fire post save操作。
     * @param entity 参数 entity
     */
    public void firePostSave(Object entity) {
        FlexMaskProcessor processor = getMaskProcessor();
        if (processor != null) processor.maskObject(entity);
        FlexAuditHandler auditHandler = getAuditHandler();
        if (auditHandler != null) auditHandler.onInsert(entity);
    }

    /**
     * 执行fire pre update操作。
     * @param entity 参数 entity
     */
    public void firePreUpdate(Object entity) {
        FlexOptimisticLockHandler handler = getOptimisticLockHandler();
        if (handler != null) handler.incrementVersion(entity);
    }

    /**
     * 执行fire post update操作。
     * @param entity 参数 entity
     */
    public void firePostUpdate(Object entity) {
        FlexMaskProcessor processor = getMaskProcessor();
        if (processor != null) processor.maskObject(entity);
        FlexAuditHandler auditHandler = getAuditHandler();
        if (auditHandler != null) auditHandler.onUpdate(entity);
    }

    /**
     * 执行fire post delete操作：将删除标记字段置为已删除值（逻辑删除）。
     *
     * @param entity 参数 entity
     * @return 成功写入删除标记返回 {@code true}；无逻辑删除处理器或实体无对应字段返回 {@code false}
     */
    public boolean firePostDelete(Object entity) {
        FlexLogicDeleteHandler handler = getLogicDeleteHandler();
        return handler != null && handler.toLogicDelete(entity);
    }

    /**
     * 执行fire post load操作：对 {@link Encrypted} 字段执行透明解密。
     *
     * @param entity 参数 entity
     */
    public void firePostLoad(Object entity) {
        FlexEncryptProcessor encryptProcessor = getEncryptProcessor();
        if (encryptProcessor != null) encryptProcessor.decryptEntity(entity);
    }

    /**
     * 「Extension」接口定义。
     *
     * @author Jowen
     * @since 0.0.1
 * @version 0.0.1
     */
    public interface Extension {
        String name();
        int order();
    }
}
