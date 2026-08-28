package cn.jowen.framework.demo.controller;

import cn.jowen.framework.demo.entity.User;
import cn.jowen.framework.demo.service.UserService;
import cn.jowen.framework.extras.common.Result;
import cn.jowen.framework.extras.web.ratelimit.RateLimit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户接口：演示数据访问 + 缓存 + 限流 + 统一响应体。
 *
 * <p>返回统一使用框架 {@link Result}，与前端约定一致的 {@code code/message/data} 结构。
 *
 * @author demo
  * @since 0.0.1
 * @version 0.0.1
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    /** log 常量。 */
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    /** userService 不可变字段。 */
    private final UserService userService;

    /**
     * 构造实例。
     * @param userService 参数 userService
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 用户列表。
     *
     * @return 全部用户
     */
    @GetMapping
    public Result<List<User>> list() {
        return Result.success(userService.findAll());
    }

    /**
     * 按主键查询：命中缓存时不会打印「回源数据库」日志。
     *
     * @param id 主键
     * @return 用户信息，不存在时返回 404 语义
     */
    @GetMapping("/{id}")
    public Result<Object> get(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) {
            return Result.fail(404, "用户不存在：" + id);
        }
        // 打印手机号，演示日志脱敏
        log.info("查询用户成功，手机号={}", user.getPhone());
        return Result.success(user);
    }

    /**
     * 新增用户（演示限流：同一 key 每分钟 10 次）。
     *
     * @param user 用户信息
     * @return 生成的主键
     */
    @PostMapping
    @RateLimit(key = "'user:create:' + #user.username", permits = 10, window = 1,
            message = "创建用户过于频繁，请稍后再试")
    public Result<Map<String, Object>> create(@RequestBody User user) {
        Long id = userService.create(user);
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        return Result.success(data);
    }

    /**
     * 更新用户。
     *
     * @param id   主键
     * @param user 用户信息
     * @return 是否成功
     */
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        return Result.success(userService.update(user));
    }

    /**
     * 删除用户。
     *
     * @param id 主键
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(userService.delete(id));
    }
}
