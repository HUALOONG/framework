package cn.jowen.framework.extras.web.captcha;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证码存储：保存待校验的验证码，支持一次性消费。
 *
 * <p>默认基于内存实现；多实例部署可替换为 Redis 实现。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface CaptchaStore {

    /**
     * 保存验证码。
     *
     * @param captcha 验证码
     */
    void save(Captcha captcha);

    /**
     * 读取验证码（不移除）。
     *
     * @param id 标识
     * @return 验证码，不存在返回 {@code null}
     */
    @Nullable Captcha get(String id);

    /**
     * 移除验证码。
     *
     * @param id 标识
     */
    void remove(String id);

    /**
     * 基于内存的默认实现。
     */
    @NullMarked
    final class InMemory implements CaptchaStore {
        /** store 不可变字段。 */
        private final Map<String, Captcha> store = new ConcurrentHashMap<>();

        /**
         * 执行save操作。
         * @param captcha 参数 captcha
         */
        @Override
        public void save(Captcha captcha) {
            store.put(captcha.id(), captcha);
        }

        /**
         * 获取。
         * @param id 参数 id
         * @return 结果
         */
        @Override
        public @Nullable Captcha get(String id) {
            return store.get(id);
        }

        /**
         * 设置ove。
         * @param id 参数 id
         */
        @Override
        public void remove(String id) {
            store.remove(id);
        }
    }
}
