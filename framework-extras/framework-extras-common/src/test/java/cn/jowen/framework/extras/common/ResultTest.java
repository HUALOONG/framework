package cn.jowen.framework.extras.common;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link Result} 测试。
 *
 * <p>覆盖：success / fail 工厂语义、isSuccess 判定、data 泛型承载、序列化可用性。
 */
class ResultTest {

    // ---------- success ----------

    @Test
    void success_withData_hasZeroCodeAndSuccessMessage() {
        Result<String> result = Result.success("payload");

        assertThat(result.getCode()).isZero();
        assertThat(result.getMessage()).isEqualTo("success");
        assertThat(result.getData()).isEqualTo("payload");
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void success_withoutData_hasNullData() {
        Result<Void> result = Result.success();

        assertThat(result.getCode()).isZero();
        assertThat(result.getMessage()).isEqualTo("success");
        assertThat(result.getData()).isNull();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void success_acceptsExplicitNullData() {
        Result<String> result = Result.success(null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNull();
    }

    @Test
    void success_carriesComplexGenericPayload() {
        Result<Map<String, List<Integer>>> result = Result.success(Map.of("a", List.of(1, 2, 3)));

        assertThat(result.getData()).containsEntry("a", List.of(1, 2, 3));
        assertThat(result.isSuccess()).isTrue();
    }

    // ---------- fail ----------

    @Test
    void fail_carriesCodeAndMessageWithoutData() {
        Result<String> result = Result.fail(4001, "参数错误");

        assertThat(result.getCode()).isEqualTo(4001);
        assertThat(result.getMessage()).isEqualTo("参数错误");
        assertThat(result.getData()).isNull();
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void fail_withNegativeCodeIsStillFailure() {
        Result<String> result = Result.fail(-1, "unknown");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo(-1);
    }

    @Test
    void isSuccess_onlyTrueForZeroCode() {
        assertThat(Result.fail(0, "手工构造 0 码").isSuccess())
                .as("isSuccess 仅以 code == 0 为判据")
                .isTrue();
        assertThat(Result.fail(1, "msg").isSuccess()).isFalse();
    }

    // ---------- 序列化 ----------

    @Test
    void result_isSerializable() throws Exception {
        Result<String> original = Result.success("serialized");

        byte[] bytes;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(original);
            oos.flush();
            bytes = bos.toByteArray();
        }

        Object restored;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            restored = ois.readObject();
        }

        assertThat(restored).isInstanceOf(Result.class);
        @SuppressWarnings("unchecked")
        Result<String> typed = (Result<String>) restored;
        assertThat(typed.getCode()).isZero();
        assertThat(typed.getMessage()).isEqualTo("success");
        assertThat(typed.getData()).isEqualTo("serialized");
        assertThat(typed.isSuccess()).isTrue();
    }
}
