package cn.jowen.framework.extras.common.exception;

import cn.jowen.framework.core.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ErrorCodeEnum} 测试。
 *
 * <p>覆盖：枚举常量完整性、code / message 契约、错误码唯一性与命名规范。
 */
class ErrorCodeEnumTest {

    @Test
    void enum_containsExactlyExpectedConstants() {
        assertThat(ErrorCodeEnum.values())
                .containsExactly(
                        ErrorCodeEnum.INVALID_ARGUMENT,
                        ErrorCodeEnum.BUSINESS_REJECTED,
                        ErrorCodeEnum.NOT_FOUND,
                        ErrorCodeEnum.OPERATION_REJECTED,
                        ErrorCodeEnum.INTERNAL_ERROR,
                        ErrorCodeEnum.DUPLICATE_REQUEST,
                        ErrorCodeEnum.LOCK_ACQUIRE_FAILED,
                        ErrorCodeEnum.RATE_LIMIT_EXCEEDED,
                        ErrorCodeEnum.NOTIFICATION_SEND_FAILED,
                        ErrorCodeEnum.STORAGE_ERROR,
                        ErrorCodeEnum.DATA_PERMISSION_DENIED,
                        ErrorCodeEnum.CAPTCHA_INVALID);
    }

    @Test
    void enum_implementsErrorCodeContract() {
        assertThat(ErrorCodeEnum.INVALID_ARGUMENT).isInstanceOf(ErrorCode.class);
    }

    @Test
    void codes_mapToDocumentedValues() {
        assertThat(ErrorCodeEnum.INVALID_ARGUMENT.code()).isEqualTo("E1001");
        assertThat(ErrorCodeEnum.BUSINESS_REJECTED.code()).isEqualTo("E1002");
        assertThat(ErrorCodeEnum.NOT_FOUND.code()).isEqualTo("E1003");
        assertThat(ErrorCodeEnum.OPERATION_REJECTED.code()).isEqualTo("E1004");
        assertThat(ErrorCodeEnum.INTERNAL_ERROR.code()).isEqualTo("E9999");
        assertThat(ErrorCodeEnum.DUPLICATE_REQUEST.code()).isEqualTo("E2001");
        assertThat(ErrorCodeEnum.LOCK_ACQUIRE_FAILED.code()).isEqualTo("E2002");
        assertThat(ErrorCodeEnum.RATE_LIMIT_EXCEEDED.code()).isEqualTo("E2003");
        assertThat(ErrorCodeEnum.NOTIFICATION_SEND_FAILED.code()).isEqualTo("E2004");
        assertThat(ErrorCodeEnum.STORAGE_ERROR.code()).isEqualTo("E2005");
        assertThat(ErrorCodeEnum.DATA_PERMISSION_DENIED.code()).isEqualTo("E2006");
        assertThat(ErrorCodeEnum.CAPTCHA_INVALID.code()).isEqualTo("E2007");
    }

    @Test
    void messages_mapToDocumentedValues() {
        assertThat(ErrorCodeEnum.INVALID_ARGUMENT.message()).isEqualTo("参数校验失败");
        assertThat(ErrorCodeEnum.BUSINESS_REJECTED.message()).isEqualTo("业务校验未通过");
        assertThat(ErrorCodeEnum.NOT_FOUND.message()).isEqualTo("资源不存在");
        assertThat(ErrorCodeEnum.OPERATION_REJECTED.message()).isEqualTo("操作被拒绝");
        assertThat(ErrorCodeEnum.INTERNAL_ERROR.message()).isEqualTo("系统内部错误");
    }

    @ParameterizedTest
    @EnumSource(ErrorCodeEnum.class)
    void everyConstant_hasNonBlankCodeAndMessage(ErrorCodeEnum value) {
        assertThat(value.code()).isNotBlank().startsWith("E").hasSize(5);
        assertThat(value.message()).isNotBlank();
    }

    @Test
    void codes_areUnique() {
        assertThat(Arrays.stream(ErrorCodeEnum.values()).map(ErrorCodeEnum::code).collect(Collectors.toSet()))
                .hasSameSizeAs(ErrorCodeEnum.values());
    }

    @Test
    void messages_areUnique() {
        assertThat(Arrays.stream(ErrorCodeEnum.values()).map(ErrorCodeEnum::message).collect(Collectors.toSet()))
                .hasSameSizeAs(ErrorCodeEnum.values());
    }

    @Test
    void valueOf_resolvesByName() {
        assertThat(ErrorCodeEnum.valueOf("NOT_FOUND")).isSameAs(ErrorCodeEnum.NOT_FOUND);
    }
}
