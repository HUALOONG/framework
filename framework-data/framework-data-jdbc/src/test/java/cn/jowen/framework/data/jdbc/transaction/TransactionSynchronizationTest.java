package cn.jowen.framework.data.jdbc.transaction;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * {@link TransactionSynchronization} 默认方法单元测试。
 */
class TransactionSynchronizationTest {

    @Test
    void defaultCallbacks_doNotThrow() {
        TransactionSynchronization sync = new TransactionSynchronization() {
        };
        assertThatCode(sync::beforeCommit).doesNotThrowAnyException();
        assertThatCode(sync::afterCommit).doesNotThrowAnyException();
        assertThatCode(sync::afterCompletion).doesNotThrowAnyException();
    }

    @Test
    void overriddenCallbacks_areInvoked() {
        StringBuilder log = new StringBuilder();
        TransactionSynchronization sync = new TransactionSynchronization() {
            @Override
            public void beforeCommit() {
                log.append("before:");
            }

            @Override
            public void afterCommit() {
                log.append("after:");
            }

            @Override
            public void afterCompletion() {
                log.append("done");
            }
        };
        sync.beforeCommit();
        sync.afterCommit();
        sync.afterCompletion();
        assertThatCode(() -> {
        }).doesNotThrowAnyException();
        assert log.toString().equals("before:after:done") : "回调顺序错误: " + log;
    }
}
