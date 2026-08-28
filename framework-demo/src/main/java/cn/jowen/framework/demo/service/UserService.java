package cn.jowen.framework.demo.service;

import cn.jowen.framework.cache.api.Cache;
import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.demo.entity.User;
import cn.jowen.framework.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户业务层：演示「缓存 + 数据访问」的典型组合。
 *
 * <p>缓存使用框架自带的 {@link CacheManager}。{@code getCache(name)} 在缓存不存在时
 * 会按默认配置自动创建一个 Caffeine 本地缓存；如需定制容量与过期时间，
 * 可在 {@code DemoCacheConfiguration} 中显式注册。
 *
 * <p><b>写策略</b>：新增/更新/删除后清除对应缓存键，保证缓存与数据库一致。
 * 更严格的场景可使用 {@code put} 主动回填。
 *
 * @author demo
  * @since 0.0.1
 * @version 0.0.1
 */
@Service
public class UserService {

    /** log 常量。 */
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    /** 缓存名，与 DemoCacheConfiguration 中注册的缓存保持一致 */
    private static final String USER_CACHE = "demo:user";

    /** userRepository 不可变字段。 */
    private final UserRepository userRepository;
    /** userCache 不可变字段。 */
    private final Cache<Long, User> userCache;

    /**
     * 构造实例。
     * @param userRepository 参数 userRepository
     * @param cacheManager 参数 cacheManager
     */
    public UserService(UserRepository userRepository, CacheManager cacheManager) {
        this.userRepository = userRepository;
        this.userCache = cacheManager.getCache(USER_CACHE);
    }

    /**
     * 按主键查询：先查缓存，未命中则回源数据库并回填。
     *
     * @param id 主键
     * @return 用户，不存在时返回 {@code null}
     */
    public User findById(Long id) {
        User cached = userCache.get(id);
        if (cached != null) {
            log.info("缓存命中，userId={}", id);
            return cached;
        }
        log.info("缓存未命中，回源数据库，userId={}", id);
        User user = userRepository.findById(id);
        if (user != null) {
            userCache.put(id, user);
            // 打印手机号用于演示日志脱敏：框架 logger 开启 desensitize 后会自动打码
            log.info("已回写缓存，用户手机号={}", user.getPhone());
        }
        return user;
    }

     /**
      * 获取find all。
      * @return 结果
      */
     * @return 结果
    /** 查询全部用户（不缓存，避免与单条缓存不一致） */
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * 新增用户。
     *
     * @param user 用户信息
     * @return 生成的主键
     */
    public Long create(User user) {
        Long id = userRepository.insert(user);
        log.info("新增用户成功，id={}，手机号={}", id, user.getPhone());
        return id;
    }

    /**
     * 更新用户并清除缓存。
     *
     * @param user 用户信息
     * @return 是否更新成功
     */
    public boolean update(User user) {
        int rows = userRepository.update(user);
        if (rows > 0 && user.getId() != null) {
            userCache.evict(user.getId());
        }
        return rows > 0;
    }

    /**
     * 删除用户并清除缓存。
     *
     * @param id 主键
     * @return 是否删除成功
     */
    public boolean delete(Long id) {
        int rows = userRepository.deleteById(id);
        if (rows > 0) {
            userCache.evict(id);
        }
        return rows > 0;
    }
}
