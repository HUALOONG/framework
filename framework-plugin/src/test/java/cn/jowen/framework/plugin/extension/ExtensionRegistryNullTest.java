package cn.jowen.framework.plugin.extension;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExtensionRegistryNullTest {

    @Test
    void getExtensionReturnsNullWhenNotFound() {
        ExtensionRegistry registry = new ExtensionRegistry();
        // 未注册任何扩展，查询应返回 null
        assertThat(registry.getExtension(TestExtensionPoint.class, "nonexistent")).isNull();
    }

    @Test
    void getExtensionsReturnsEmptyWhenNoImplementations() {
        ExtensionRegistry registry = new ExtensionRegistry();
        // 未注册任何扩展，查询应返回空列表
        assertThat(registry.getExtensions(TestExtensionPoint.class)).isEmpty();
    }

    @ExtensionPoint
    interface TestExtensionPoint {}
}
