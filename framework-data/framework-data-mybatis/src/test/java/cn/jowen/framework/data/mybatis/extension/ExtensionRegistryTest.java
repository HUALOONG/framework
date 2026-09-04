package cn.jowen.framework.data.mybatis.extension;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExtensionRegistryTest {

    @AfterEach
    void resetKey() {
        // FlexEncryptProcessor 密钥为静态共享，避免跨测试污染
        FlexEncryptProcessor.setDefaultKey(null);
    }

    @Test
    void defaults_registersAllExtensions() {
        ExtensionRegistry registry = ExtensionRegistry.defaults();
        List<ExtensionRegistry.Extension> all = registry.getAll();
        assertThat(all).hasSize(7);
    }

    @Test
    void defaults_extensionsOrdered() {
        ExtensionRegistry registry = ExtensionRegistry.defaults();
        List<ExtensionRegistry.Extension> all = registry.getAll();
        for (int i = 0; i < all.size() - 1; i++) {
            assertThat(all.get(i).order()).isLessThanOrEqualTo(all.get(i + 1).order());
        }
    }

    @Test
    void register_deduplicates() {
        ExtensionRegistry registry = new ExtensionRegistry();
        registry.register(new FlexTenantHandler());
        registry.register(new FlexTenantHandler());
        assertThat(registry.getAll()).hasSize(1);
    }

    @Test
    void register_nullThrows() {
        assertThatThrownBy(() -> new ExtensionRegistry().register(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getTenantHandler() {
        ExtensionRegistry registry = ExtensionRegistry.defaults();
        FlexTenantHandler handler = registry.getTenantHandler();
        assertThat(handler).isNotNull();
    }

    @Test
    void getMaskProcessor() {
        ExtensionRegistry registry = ExtensionRegistry.defaults();
        FlexMaskProcessor processor = registry.getMaskProcessor();
        assertThat(processor).isNotNull();
    }

    @Test
    void getAuditHandler() {
        ExtensionRegistry registry = ExtensionRegistry.defaults();
        FlexAuditHandler handler = registry.getAuditHandler();
        assertThat(handler).isNotNull();
    }

    @Test
    void getEncryptProcessor() {
        ExtensionRegistry registry = ExtensionRegistry.defaults();
        FlexEncryptProcessor processor = registry.getEncryptProcessor();
        assertThat(processor).isNotNull();
    }

    @Test
    void getSqlAuditListener() {
        ExtensionRegistry registry = ExtensionRegistry.defaults();
        FlexSqlAuditListener listener = registry.getSqlAuditListener();
        assertThat(listener).isNotNull();
    }

    @Test
    void getOptimisticLockHandler() {
        ExtensionRegistry registry = ExtensionRegistry.defaults();
        FlexOptimisticLockHandler handler = registry.getOptimisticLockHandler();
        assertThat(handler).isNotNull();
    }

    @Test
    void getLogicDeleteHandler() {
        ExtensionRegistry registry = ExtensionRegistry.defaults();
        FlexLogicDeleteHandler handler = registry.getLogicDeleteHandler();
        assertThat(handler).isNotNull();
    }

    @Test
    void firePostSave_maskObject() {
        ExtensionRegistry registry = ExtensionRegistry.defaults();
        // Just ensure no exception is thrown
        assertThatCode(() -> registry.firePostSave(new Object())).doesNotThrowAnyException();
    }

    @Test
    void get_returnsOptional_byName() {
        ExtensionRegistry registry = ExtensionRegistry.defaults();
        assertThat(registry.get("audit")).isPresent();
        assertThat(registry.get("audit").get()).isInstanceOf(FlexAuditHandler.class);
        assertThat(registry.get("does-not-exist")).isEmpty();
    }

    @Test
    void firePreUpdate_incrementsVersion() {
        ExtensionRegistry registry = ExtensionRegistry.defaults();
        VersionedEntity entity = new VersionedEntity();
        entity.version = 1;
        registry.firePreUpdate(entity);
        assertThat(entity.version).isEqualTo(2);
    }

    @Test
    void firePostUpdate_maskAndAudit_noop_whenUnconfigured() {
        ExtensionRegistry registry = ExtensionRegistry.defaults();
        // SecureEntity has no @Masked field and audit on Object() is safe
        assertThatCode(() -> registry.firePostUpdate(new Object())).doesNotThrowAnyException();
    }

    static final class VersionedEntity {
        Integer version;
        Integer getVersion() { return version; }
        void setVersion(Integer v) { this.version = v; }
    }

    // ===== P0-C2 fire 链 =====

    @Test
    void firePreSave_encryptsAndInjectsTenant_whenConfigured() {
        FlexEncryptProcessor.setDefaultKey(dummyKey());
        ExtensionRegistry registry = ExtensionRegistry.defaults();
        SecureEntity entity = new SecureEntity(null, "plaintext");

        FlexTenantHandler.runWithTenant("tenant-A", () -> registry.firePreSave(entity));

        assertThat(entity.secret).isNotEqualTo("plaintext"); // 已加密
        assertThat(entity.tenantId).isEqualTo("tenant-A");    // 已注入租户
    }

    @Test
    void firePreSave_safeNoop_whenUnconfigured() {
        FlexEncryptProcessor.setDefaultKey(null);
        ExtensionRegistry registry = ExtensionRegistry.defaults();
        SecureEntity entity = new SecureEntity(null, "plaintext");

        registry.firePreSave(entity);

        assertThat(entity.secret).isEqualTo("plaintext"); // 未配置：不加密
        assertThat(entity.tenantId).isNull();             // 无租户上下文：不注入
    }

    @Test
    void firePostDelete_logicDeletes_whenFieldPresent() {
        ExtensionRegistry registry = ExtensionRegistry.defaults();
        DeletableEntity entity = new DeletableEntity();
        boolean applied = registry.firePostDelete(entity);
        assertThat(applied).isTrue();
        assertThat(entity.deleted).isEqualTo(1);
    }

    @Test
    void firePostDelete_false_whenNoField() {
        ExtensionRegistry registry = ExtensionRegistry.defaults();
        assertThat(registry.firePostDelete(new Object())).isFalse();
    }

    @Test
    void firePostLoad_decrypts_whenConfigured() {
        FlexEncryptProcessor.setDefaultKey(dummyKey());
        ExtensionRegistry registry = ExtensionRegistry.defaults();
        SecureEntity entity = new SecureEntity("u1", "plaintext");
        registry.firePreSave(entity);          // 先加密
        String cipher = entity.secret;
        registry.firePostLoad(entity);         // 读取后解密
        assertThat(entity.secret).isEqualTo("plaintext");
        assertThat(cipher).isNotEqualTo("plaintext");
    }

    @Test
    void firePostLoad_safeNoop_whenUnconfigured() {
        FlexEncryptProcessor.setDefaultKey(null);
        ExtensionRegistry registry = ExtensionRegistry.defaults();
        SecureEntity entity = new SecureEntity("u1", "plaintext");
        registry.firePostLoad(entity);
        assertThat(entity.secret).isEqualTo("plaintext");
    }

    private static String dummyKey() {
        byte[] key = new byte[16];
        new java.security.SecureRandom().nextBytes(key);
        return java.util.Base64.getEncoder().encodeToString(key);
    }

    static final class SecureEntity {
        String tenantId;
        @Encrypted String secret;

        SecureEntity(String tenantId, String secret) {
            this.tenantId = tenantId;
            this.secret = secret;
        }

        public String getTenantId() { return tenantId; }
        public void setTenantId(String v) { this.tenantId = v; }
    }

    static final class DeletableEntity {
        Integer deleted;
        Integer getDeleted() { return deleted; }
        void setDeleted(Integer v) { this.deleted = v; }
    }
}
