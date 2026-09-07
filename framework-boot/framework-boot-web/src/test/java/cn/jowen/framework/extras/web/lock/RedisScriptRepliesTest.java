package cn.jowen.framework.extras.web.lock;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link RedisScriptReplies} 单元测试：验证不同客户端返回值类型的收敛规则。
 *
 * @author 王飞
 * @since 0.0.2
 * @version 0.0.2
 */
@NullMarked
class RedisScriptRepliesTest {

    @Test
    void toLong_withNullReply_returnsZero() {
        assertThat(RedisScriptReplies.toLong(null)).isZero();
    }

    @Test
    void toLong_withLongReply_returnsItsValue() {
        assertThat(RedisScriptReplies.toLong(42L)).isEqualTo(42L);
        assertThat(RedisScriptReplies.toLong(0L)).isZero();
        assertThat(RedisScriptReplies.toLong(-1L)).isEqualTo(-1L);
    }

    @Test
    void toLong_withIntegerReply_returnsItsValue() {
        assertThat(RedisScriptReplies.toLong(7)).isEqualTo(7L);
    }

    @Test
    void toLong_withDoubleReply_truncatesToLong() {
        assertThat(RedisScriptReplies.toLong(3.9d)).isEqualTo(3L);
    }

    @Test
    void toLong_withNumericStringReply_parsesValue() {
        assertThat(RedisScriptReplies.toLong("12")).isEqualTo(12L);
        assertThat(RedisScriptReplies.toLong("-3")).isEqualTo(-3L);
    }

    @Test
    void toLong_withBooleanReply_mapsTrueToOneAndFalseToZero() {
        assertThat(RedisScriptReplies.toLong(Boolean.TRUE)).isEqualTo(1L);
        assertThat(RedisScriptReplies.toLong(Boolean.FALSE)).isZero();
    }

    @Test
    void toLong_withUnsupportedType_throwsIllegalArgumentExceptionContainingTypeName() {
        List<String> unsupported = List.of("a", "b");

        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> RedisScriptReplies.toLong(unsupported));

        assertThat(thrown).hasMessageContaining(unsupported.getClass().getName())
                .hasMessageContaining("Unsupported Redis script reply type");
    }

    @Test
    void toLong_withNonNumericString_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> RedisScriptReplies.toLong("not-a-number"));
    }

    @Test
    void isAllowed_withPositiveReply_returnsTrue() {
        assertThat(RedisScriptReplies.isAllowed(1L)).isTrue();
        assertThat(RedisScriptReplies.isAllowed(5)).isTrue();
        assertThat(RedisScriptReplies.isAllowed("1")).isTrue();
        assertThat(RedisScriptReplies.isAllowed(Boolean.TRUE)).isTrue();
    }

    @Test
    void isAllowed_withZeroOrNullReply_returnsFalse() {
        assertThat(RedisScriptReplies.isAllowed(0L)).isFalse();
        assertThat(RedisScriptReplies.isAllowed(0)).isFalse();
        assertThat(RedisScriptReplies.isAllowed("0")).isFalse();
        assertThat(RedisScriptReplies.isAllowed(Boolean.FALSE)).isFalse();
        assertThat(RedisScriptReplies.isAllowed(null)).isFalse();
    }

    @Test
    void toReplyString_withNullReply_returnsNull() {
        assertThat(RedisScriptReplies.toReplyString(null)).isNull();
    }

    @Test
    void toReplyString_withNonNullReply_returnsStringForm() {
        assertThat(RedisScriptReplies.toReplyString(3L)).isEqualTo("3");
        assertThat(RedisScriptReplies.toReplyString("OK")).isEqualTo("OK");
        assertThat(RedisScriptReplies.toReplyString(Boolean.TRUE)).isEqualTo("true");
    }

    @Test
    void utilityClass_hasSinglePrivateConstructorThatRefusesInstantiation() throws Exception {
        Constructor<?>[] ctors = RedisScriptReplies.class.getDeclaredConstructors();
        assertThat(ctors).hasSize(1);
        assertThat(Modifier.isPrivate(ctors[0].getModifiers())).isTrue();

        Constructor<RedisScriptReplies> ctor = RedisScriptReplies.class.getDeclaredConstructor();
        ctor.setAccessible(true);

        InvocationTargetException wrapper = assertThrows(
                InvocationTargetException.class,
                ctor::newInstance);
        assertThat(wrapper.getCause()).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must not be instantiated");
    }
}
