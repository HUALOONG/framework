package cn.jowen.framework.extras.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * extras 异常体系单元测试。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class ExtrasExceptionTest {

    @Test
    void shouldUseDomainDefaultErrorCode() {
        assertThat(new LockException("lock failed").getErrorCode()).isEqualTo(ErrorCodeEnum.LOCK_ACQUIRE_FAILED);
        assertThat(new RateLimitException("too many").getErrorCode()).isEqualTo(ErrorCodeEnum.RATE_LIMIT_EXCEEDED);
        assertThat(new IdempotencyException("duplicate").getErrorCode()).isEqualTo(ErrorCodeEnum.DUPLICATE_REQUEST);
        assertThat(new StorageException("io error").getErrorCode()).isEqualTo(ErrorCodeEnum.STORAGE_ERROR);
        assertThat(new NotificationException("send failed").getErrorCode()).isEqualTo(ErrorCodeEnum.NOTIFICATION_SEND_FAILED);
        assertThat(new CaptchaException("mismatch").getErrorCode()).isEqualTo(ErrorCodeEnum.CAPTCHA_INVALID);
        assertThat(new DataPermissionException("denied").getErrorCode()).isEqualTo(ErrorCodeEnum.DATA_PERMISSION_DENIED);
    }

    @Test
    void shouldKeepCustomMessage() {
        LockException exception = new LockException("获取订单锁失败");

        assertThat(exception).hasMessage("获取订单锁失败");
    }

    @Test
    void shouldKeepCause() {
        Throwable cause = new IllegalStateException("redis down");
        StorageException exception = new StorageException("上传失败", cause);

        assertThat(exception).hasCause(cause);
        assertThat(exception.getMessage()).isEqualTo("上传失败");
    }

    @Test
    void shouldSupportCustomErrorCode() {
        CaptchaException exception = new CaptchaException(ErrorCodeEnum.CAPTCHA_INVALID, "验证码无效或已过期");

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCodeEnum.CAPTCHA_INVALID);
        assertThat(exception).hasMessage("验证码无效或已过期");
    }

    @Test
    void shouldBeSubclassOfExtrasException() {
        assertThat(new LockException("x")).isInstanceOf(ExtrasException.class);
        assertThat(new RateLimitException("x")).isInstanceOf(ExtrasException.class);
        assertThat(new IdempotencyException("x")).isInstanceOf(ExtrasException.class);
        assertThat(new StorageException("x")).isInstanceOf(ExtrasException.class);
        assertThat(new NotificationException("x")).isInstanceOf(ExtrasException.class);
        assertThat(new CaptchaException("x")).isInstanceOf(ExtrasException.class);
        assertThat(new DataPermissionException("x")).isInstanceOf(ExtrasException.class);
    }

    @Test
    void shouldExposeErrorCodeValue() {
        assertThat(ErrorCodeEnum.LOCK_ACQUIRE_FAILED.code()).isEqualTo("E2002");
        assertThat(ErrorCodeEnum.LOCK_ACQUIRE_FAILED.message()).isEqualTo("资源锁获取失败");
        assertThat(ErrorCodeEnum.INTERNAL_ERROR.code()).isEqualTo("E9999");
    }

    @Test
    void shouldUseErrorCodeMessageWhenOnlyCodeGiven() {
        ExtrasException exception = new ExtrasException(ErrorCodeEnum.NOT_FOUND);

        assertThat(exception).hasMessage("资源不存在");
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCodeEnum.NOT_FOUND);
    }
}
