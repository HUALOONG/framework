package cn.jowen.framework.extras.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ExtrasException} 根类全部构造器的契约测试。
 *
 * <p>{@link ExtrasExceptionTest} 覆盖的是各业务子类路径，本类补充根类自身被直接使用时的
 * 消息/原因/错误码组合语义——尤其是"仅错误码"与"错误码 + 原因"两种构造，
 * 它们决定了调用方能否还原底层故障。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class ExtrasExceptionConstructorsTest {

    @Test
    void messageOnly_preservesMessageWithoutCause() {
        ExtrasException exception = new ExtrasException("裸消息");

        assertThat(exception).hasMessage("裸消息");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void messageAndCause_preservesBoth() {
        RuntimeException cause = new RuntimeException("底层异常");

        ExtrasException exception = new ExtrasException("外层消息", cause);

        assertThat(exception).hasMessage("外层消息");
        assertThat(exception).hasCause(cause);
    }

    @Test
    void errorCodeAndCause_fallsBackToErrorCodeMessage() {
        IllegalStateException cause = new IllegalStateException("连接池耗尽");

        ExtrasException exception = new ExtrasException(ErrorCodeEnum.NOT_FOUND, cause);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCodeEnum.NOT_FOUND);
        assertThat(exception).hasMessage("资源不存在");
        assertThat(exception).hasCause(cause);
    }
}
