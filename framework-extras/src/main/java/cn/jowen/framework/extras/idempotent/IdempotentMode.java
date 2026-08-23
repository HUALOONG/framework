package cn.jowen.framework.extras.idempotent;

import org.jspecify.annotations.NullMarked;

/**
 * 幂等模式枚举。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public enum IdempotentMode {
    /** Token 模式：请求头携带 Token，校验后删除 */
    TOKEN,
    /** Key 模式：使用业务键进行 SETNX 原子操作 */
    KEY
}
