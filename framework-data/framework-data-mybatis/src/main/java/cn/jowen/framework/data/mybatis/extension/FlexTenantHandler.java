package cn.jowen.framework.data.mybatis.extension;

import cn.jowen.framework.data.core.datasource.DataSourceContext;
import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class FlexTenantHandler implements ExtensionRegistry.Extension {

    private static final Logger logger = LoggerFactory.getLogger(FlexTenantHandler.class);

    static final String CONTEXT_KEY = "framework.data.tenant.id";
    static final String TENANT_COLUMN = "tenant_id";

    @Override public String name() { return "tenant"; }
    @Override public int order() { return 100; }

    @Nullable
    public static String getCurrentTenantId() {
        return DataSourceContext.getDataSourceKey();
    }

    public static void runWithTenant(String tenantId, Runnable task) {
        DataSourceContext.runWith(tenantId, task);
    }

    public String tenantCondition() {
        String tenantId = getCurrentTenantId();
        return (tenantId == null) ? "" : TENANT_COLUMN + " = '" + tenantId + "'";
    }

    public void injectTenant(Object entity, String field) {
        String tenantId = getCurrentTenantId();
        if (tenantId == null) return;
        try {
            String setterName = "set" + Character.toUpperCase(field.charAt(0)) + field.substring(1);
            var setter = entity.getClass().getMethod(setterName, String.class);
            setter.invoke(entity, tenantId);
        } catch (Exception e) {
            logger.debug("注入租户字段失败 (" + field + "): " + e.getMessage());
        }
    }
}
