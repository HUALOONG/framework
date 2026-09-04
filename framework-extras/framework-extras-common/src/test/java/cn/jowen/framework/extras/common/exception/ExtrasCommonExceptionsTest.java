package cn.jowen.framework.extras.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 覆盖 extras-common 下各业务异常的全部构造器分支（默认错误码、带 cause、指定错误码）。
 */
class ExtrasCommonExceptionsTest {

    @Test
    void rateLimitException() {
        RateLimitException e1 = new RateLimitException("r");
        assertThat(e1.getMessage()).isEqualTo("r");
        assertThat(e1.getErrorCode()).isEqualTo(ErrorCodeEnum.RATE_LIMIT_EXCEEDED);

        RateLimitException e2 = new RateLimitException("r", new RuntimeException("c"));
        assertThat(e2.getMessage()).isEqualTo("r");
        assertThat(e2.getCause()).hasMessage("c");

        RateLimitException e3 = new RateLimitException(ErrorCodeEnum.LOCK_ACQUIRE_FAILED, "custom");
        assertThat(e3.getMessage()).isEqualTo("custom");
        assertThat(e3.getErrorCode()).isEqualTo(ErrorCodeEnum.LOCK_ACQUIRE_FAILED);
    }

    @Test
    void notificationException() {
        NotificationException e1 = new NotificationException("n");
        assertThat(e1.getMessage()).isEqualTo("n");
        NotificationException e2 = new NotificationException("n", new RuntimeException("c"));
        assertThat(e2.getCause()).hasMessage("c");
        NotificationException e3 = new NotificationException(ErrorCodeEnum.STORAGE_ERROR, "custom");
        assertThat(e3.getErrorCode()).isEqualTo(ErrorCodeEnum.STORAGE_ERROR);
    }

    @Test
    void lockException() {
        LockException e1 = new LockException("l");
        assertThat(e1.getMessage()).isEqualTo("l");
        LockException e2 = new LockException("l", new RuntimeException("c"));
        assertThat(e2.getCause()).hasMessage("c");
        LockException e3 = new LockException(ErrorCodeEnum.LOCK_ACQUIRE_FAILED, "custom");
        assertThat(e3.getErrorCode()).isEqualTo(ErrorCodeEnum.LOCK_ACQUIRE_FAILED);
    }

    @Test
    void idempotencyException() {
        IdempotencyException e1 = new IdempotencyException("i");
        assertThat(e1.getMessage()).isEqualTo("i");
        IdempotencyException e2 = new IdempotencyException("i", new RuntimeException("c"));
        assertThat(e2.getCause()).hasMessage("c");
        IdempotencyException e3 = new IdempotencyException(ErrorCodeEnum.STORAGE_ERROR, "custom");
        assertThat(e3.getErrorCode()).isEqualTo(ErrorCodeEnum.STORAGE_ERROR);
    }

    @Test
    void dataPermissionException() {
        DataPermissionException e1 = new DataPermissionException("d");
        assertThat(e1.getMessage()).isEqualTo("d");
        DataPermissionException e2 = new DataPermissionException("d", new RuntimeException("c"));
        assertThat(e2.getCause()).hasMessage("c");
        DataPermissionException e3 = new DataPermissionException(ErrorCodeEnum.STORAGE_ERROR, "custom");
        assertThat(e3.getErrorCode()).isEqualTo(ErrorCodeEnum.STORAGE_ERROR);
    }

    @Test
    void captchaException() {
        CaptchaException e1 = new CaptchaException("c");
        assertThat(e1.getMessage()).isEqualTo("c");
        assertThat(e1.getErrorCode()).isEqualTo(ErrorCodeEnum.CAPTCHA_INVALID);
        CaptchaException e2 = new CaptchaException("c", new RuntimeException("x"));
        assertThat(e2.getCause()).hasMessage("x");
        CaptchaException e3 = new CaptchaException(ErrorCodeEnum.CAPTCHA_INVALID, "custom");
        assertThat(e3.getErrorCode()).isEqualTo(ErrorCodeEnum.CAPTCHA_INVALID);
    }

    @Test
    void storageException() {
        StorageException e1 = new StorageException("s");
        assertThat(e1.getMessage()).isEqualTo("s");
        assertThat(e1.getErrorCode()).isEqualTo(ErrorCodeEnum.STORAGE_ERROR);
        StorageException e2 = new StorageException("s", new RuntimeException("x"));
        assertThat(e2.getCause()).hasMessage("x");
        StorageException e3 = new StorageException(ErrorCodeEnum.STORAGE_ERROR, "custom");
        assertThat(e3.getErrorCode()).isEqualTo(ErrorCodeEnum.STORAGE_ERROR);
    }
}
