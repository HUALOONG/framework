package cn.jowen.framework.data.mybatis.extension;

import cn.jowen.framework.data.core.datasource.DataSourceContext;
import cn.jowen.framework.data.mybatis.extension.ExtensionRegistry.Extension;
import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class FlexTenantHandler implements Extension {

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

    /**
     * 返回参数化的租户过滤条件（占位符形式），避免在 SQL 中字符串拼接租户 ID 造成注入风险。
     *
     * <p>调用方需将 {@link #getCurrentTenantId()} 作为对应参数绑定到占位符，例如：
     * <pre>
     *   String cond = handler.tenantCondition();      // "tenant_id = ?"
     *   // 执行时绑定参数 handler.getCurrentTenantId()
     * </pre>
     * 当未设置租户标识时返回空串，调用方应据此跳过条件拼接。
     */
    public String tenantCondition() {
        return getCurrentTenantId() == null ? "" : TENANT_COLUMN + " = ?";
    }

    /**
     * 租户 ID 格式白名单校验：仅允许数字、大小写字母、短横线与点号，
     * 作为参数化之外的纵深防御，防止异常租户标识进入查询。
     *
     * @param tenantId 待校验租户标识，可为 {@code null}
     * @return 合法返回 {@code true}
     */
    public static boolean isValidTenantId(@Nullable String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            return false;
        }
        for (int i = 0; i < tenantId.length(); i++) {
            char c = tenantId.charAt(i);
            boolean ok = (c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || c == '-' || c == '.';
            if (!ok) {
                return false;
            }
        }
        return true;
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
