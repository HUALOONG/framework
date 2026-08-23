package cn.jowen.framework.data.mybatis.extension;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExtensionRegistryTest {

    @Test
    void defaults_registersAllExtensions() {
        ExtensionRegistry registry = ExtensionRegistry.defaults();
        List<ExtensionRegistry.Extension> all = registry.getAll();
        assertThat(all).hasSize(6);
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
}
