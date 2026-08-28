package cn.jowen.framework.extras.storage.exception;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link StorageException} 异常契约验证。
 */
class StorageExceptionTest {

    @Test
    void isExtrasExceptionSubtype() {
        assertThat(new StorageException("boom")).isInstanceOf(ExtrasException.class);
    }

    @Test
    void messageConstructorCarriesMessage() {
        StorageException ex = new StorageException("文件不存在");
        assertThat(ex.getMessage()).isEqualTo("文件不存在");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void causeConstructorChainsCause() {
        IllegalStateException root = new IllegalStateException("io error");
        StorageException ex = new StorageException("读取失败", root);

        assertThat(ex.getMessage()).isEqualTo("读取失败");
        assertThat(ex.getCause()).isSameAs(root);
    }

    @Test
    void canBeThrownAndCaughtAsExtrasException() {
        assertThatThrownBy(() -> {
            throw new StorageException("拒绝访问");
        }).isInstanceOf(ExtrasException.class)
                .hasMessage("拒绝访问");
    }
}
