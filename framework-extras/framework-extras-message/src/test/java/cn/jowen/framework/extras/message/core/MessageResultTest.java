package cn.jowen.framework.extras.message.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MessageResult} 工厂与契约验证（不可变 record）。
 */
class MessageResultTest {

    @Test
    void successWithoutTaskIdHasNullFields() {
        MessageResult result = MessageResult.success();

        assertThat(result.successful()).isTrue();
        assertThat(result.taskId()).isNull();
        assertThat(result.error()).isNull();
    }

    @Test
    void successWithTaskIdCarriesTaskId() {
        MessageResult result = MessageResult.success("task-123");

        assertThat(result.successful()).isTrue();
        assertThat(result.taskId()).isEqualTo("task-123");
        assertThat(result.error()).isNull();
    }

    @Test
    void failureCarriesErrorMessageAndNullTaskId() {
        MessageResult result = MessageResult.failure("网络异常");

        assertThat(result.successful()).isFalse();
        assertThat(result.taskId()).isNull();
        assertThat(result.error()).isEqualTo("网络异常");
    }

    @Test
    void factoryMethodsReturnDistinctInstances() {
        MessageResult a = MessageResult.success("t1");
        MessageResult b = MessageResult.success("t2");

        assertThat(a).isNotEqualTo(b);
        assertThat(a.taskId()).isEqualTo("t1");
        assertThat(b.taskId()).isEqualTo("t2");
    }

    @Test
    void recordEqualityIsValueBased() {
        assertThat(MessageResult.success("x")).isEqualTo(MessageResult.success("x"));
        assertThat(MessageResult.failure("e")).isEqualTo(MessageResult.failure("e"));
        assertThat(MessageResult.success("x")).isNotEqualTo(MessageResult.failure("e"));
    }

    @Test
    void nullArgumentsAreAccepted() {
        MessageResult result = MessageResult.success(null);
        assertThat(result.taskId()).isNull();
        assertThat(result.successful()).isTrue();

        MessageResult failed = MessageResult.failure(null);
        assertThat(failed.error()).isNull();
        assertThat(failed.successful()).isFalse();
    }
}
