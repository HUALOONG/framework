package cn.jowen.framework.extras.idempotent;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * 幂等令牌生成器。
 *
 * <p>支持 UUID 和 Snowflake 两种生成策略。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class IdempotentTokenGenerator {

    /**
     * 生成 UUID 风格的令牌。
     *
     * @return 32位小写十六进制字符串
     */
    public static String generateUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    // TODO: 实现 Snowflake 生成策略
}
