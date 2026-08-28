package cn.jowen.framework.demo.config;

import cn.jowen.framework.extras.properties.DataScope;
import cn.jowen.framework.extras.web.datapermission.DataPermissionRule;
import cn.jowen.framework.extras.web.operatelog.OperatorProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.jspecify.annotations.Nullable;

/**
 * 演示：为 boot-web 的可选扩展点提供业务实现。
 *
 * <p>框架默认已注册 {@code OperatorProvider(NONE)} 与 {@code DataPermissionRule(Default)}，
 * 二者均为无条件覆盖型 Bean，因此本配置使用 {@link Primary} 使其优先注入到对应切面。</p>
  * @author Jowen
  * @since 0.0.1
 * @version 0.0.1
 */
@Configuration
public class ExtrasFeatureConfig {

    /**
     * 提供操作日志的操作人。
     * 生产环境通常从 Spring Security / OAuth2 的认证信息中解析。
     * @return 结果
     */
    @Bean
    @Primary
    public OperatorProvider demoOperatorProvider() {
        return () -> "demo-user";
    }

    /**
     * 演示数据权限规则：当前登录用户只能访问自己范围的数据。
     *
     * <p>这里只返回 SQL 条件片段，真正的参数替换与 SQL 注入由持久层适配器
     * （JDBC / MyBatis 拦截器）结合 {@link DataPermissionRule} 完成。</p>
     * @return 结果
     */
    @Bean
    @Primary
    public DataPermissionRule demoDataPermissionRule() {
        return new DataPermissionRule() {
            @Override
            public @Nullable String condition(DataScope scope, String table) {
                if (scope == DataScope.SELF) {
                    // 演示条件：只返回 owner = 当前用户 的数据
                    String t = (table == null || table.isEmpty()) ? "t" : table;
                    return t + ".owner = 'demo-user'";
                }
                // 其他范围交回默认（ALL 返回 null 不过滤，其余返回空条件）
                return new DataPermissionRule.Default().condition(scope, table);
            }
        };
    }
}
