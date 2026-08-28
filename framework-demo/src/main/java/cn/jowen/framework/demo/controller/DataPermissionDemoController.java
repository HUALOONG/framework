package cn.jowen.framework.demo.controller;

import cn.jowen.framework.extras.common.Result;
import cn.jowen.framework.extras.properties.DataScope;
import cn.jowen.framework.extras.web.datapermission.DataPermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据权限示例。
 *
 * <p>{@link DataPermission} 声明该方法需要按 {@link Scope#SELF} 过滤数据。
 * 实际 SQL 条件由 {@code ExtrasFeatureConfig} 中注入的 {@code DataPermissionRule}
 * （返回 {@code t.owner = 'demo-user'}）提供，并由持久层适配器注入到查询中。</p>
 *
 * <p>注意：demo 使用 JDBC（无 MyBatis 拦截器），此处仅演示注解 + 规则注入；
 * 真实数据过滤请结合 JDBC/MyBatis 的数据权限适配器实现。</p>
  * @author Jowen
  * @since 0.0.1
 * @version 0.0.1
 */
@RestController
@RequestMapping("/demo/data-permission")
public class DataPermissionDemoController {

    /** String 字段。 */
    @DataPermission(scope = DataScope.SELF, table = "t_order")
    @GetMapping("/orders")
    public Result<String> myOrders() {
        return Result.success("查询已附加数据权限条件：t_order.owner = 'demo-user'");
    }
}
