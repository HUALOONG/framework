package cn.jowen.framework.extras.common.exception;

import cn.jowen.framework.core.exception.ErrorCode;
import org.jspecify.annotations.NullMarked;

/**
 * 扩展模块通用错误码枚举。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public enum ErrorCodeEnum implements ErrorCode {

    /** 参数校验失败 */
    INVALID_ARGUMENT("E1001", "参数校验失败"),
    /** 业务校验未通过 */
    BUSINESS_REJECTED("E1002", "业务校验未通过"),
    /** 资源未找到 */
    NOT_FOUND("E1003", "资源不存在"),
    /** 操作被拒绝 */
    OPERATION_REJECTED("E1004", "操作被拒绝"),
    /** 系统内部错误 */
    INTERNAL_ERROR("E9999", "系统内部错误"),
    /** 重复请求（幂等防重命中） */
    DUPLICATE_REQUEST("E2001", "重复请求，请勿重复提交"),
    /** 锁获取失败 */
    LOCK_ACQUIRE_FAILED("E2002", "资源锁获取失败"),
    /** 限流触发 */
    RATE_LIMIT_EXCEEDED("E2003", "请求过于频繁，请稍后再试"),
    /** 通知发送失败 */
    NOTIFICATION_SEND_FAILED("E2004", "通知发送失败"),
    /** 存储操作失败 */
    STORAGE_ERROR("E2005", "存储操作失败"),
    /** 数据权限不足 */
    DATA_PERMISSION_DENIED("E2006", "数据权限不足"),
    /** 验证码无效 */
    CAPTCHA_INVALID("E2007", "验证码无效或已过期");

    private final String code;
    private final String message;

    ErrorCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
