package cn.jowen.framework.data.mybatis.exception;

import cn.jowen.framework.data.core.exception.OptimisticLockException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link FlexOptimisticLockException} 测试：覆盖全部构造器，并校验异常分类归属。
 *
 * <p>乐观锁冲突属于可重试错误，调用方应据此决定重试而非按不可恢复故障处理，
 * 因此除消息与原因链外还断言其在持久层异常树中的位置。
 */
class FlexOptimisticLockExceptionTest {

    @Test
    void messageOnly_preservesMessage() {
        FlexOptimisticLockException ex = new FlexOptimisticLockException("version conflict");

        assertThat(ex.getMessage()).isEqualTo("version conflict");
        assertThat(ex.getCause()).isNull();
        assertThat(ex).isInstanceOf(OptimisticLockException.class);
    }

    @Test
    void messageAndCause_preservesUnderlyingSqlError() {
        SQLException cause = new SQLException("update affected 0 rows");
        FlexOptimisticLockException ex = new FlexOptimisticLockException("version conflict", cause);

        assertThat(ex.getMessage()).isEqualTo("version conflict");
        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}
