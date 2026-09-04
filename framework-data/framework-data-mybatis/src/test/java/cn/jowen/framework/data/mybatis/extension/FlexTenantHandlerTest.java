package cn.jowen.framework.data.mybatis.extension;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlexTenantHandlerTest {

    static class Entity {
        String tenantId;
        public void setTenantId(String t) { this.tenantId = t; }
    }

    @Test
    void name_and_order() {
        FlexTenantHandler handler = new FlexTenantHandler();
        assertThat(handler.name()).isEqualTo("tenant");
        assertThat(handler.order()).isEqualTo(100);
    }

    @Test
    void getCurrentTenantId_scoped() {
        assertThat(FlexTenantHandler.getCurrentTenantId()).isNull();
        FlexTenantHandler.runWithTenant("t1", () ->
                assertThat(FlexTenantHandler.getCurrentTenantId()).isEqualTo("t1"));
        assertThat(FlexTenantHandler.getCurrentTenantId()).isNull();
    }

    @Test
    void isValidTenantId_whitelist() {
        assertThat(FlexTenantHandler.isValidTenantId("abc-123.X")).isTrue();
        assertThat(FlexTenantHandler.isValidTenantId(null)).isFalse();
        assertThat(FlexTenantHandler.isValidTenantId("")).isFalse();
        assertThat(FlexTenantHandler.isValidTenantId("a b")).isFalse();
        assertThat(FlexTenantHandler.isValidTenantId("a@b")).isFalse();
    }

    @Test
    void tenantCondition_dependsOnCurrentTenant() {
        FlexTenantHandler handler = new FlexTenantHandler();
        assertThat(handler.tenantCondition()).isEmpty();
        FlexTenantHandler.runWithTenant("acme", () ->
                assertThat(handler.tenantCondition()).isEqualTo("tenant_id = ?"));
    }

    @Test
    void injectTenant_setsFieldWhenTenantPresent() {
        FlexTenantHandler handler = new FlexTenantHandler();
        FlexTenantHandler.runWithTenant("acme", () -> {
            Entity e = new Entity();
            handler.injectTenant(e, "tenantId");
            assertThat(e.tenantId).isEqualTo("acme");
        });
    }

    @Test
    void injectTenant_noOpWhenNoTenant() {
        FlexTenantHandler handler = new FlexTenantHandler();
        Entity e = new Entity();
        e.tenantId = "orig";
        handler.injectTenant(e, "tenantId");
        assertThat(e.tenantId).isEqualTo("orig");
    }
}
