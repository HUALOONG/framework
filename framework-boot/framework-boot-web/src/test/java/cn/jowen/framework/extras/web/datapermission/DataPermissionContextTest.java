package cn.jowen.framework.extras.web.datapermission;

import cn.jowen.framework.extras.properties.DataScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DataPermissionContext} 与 {@link DataPermissionRule.Default} 测试。
 *
 * <p>覆盖：线程变量读写、默认值兜底、null 表名归一化、clear 语义、线程隔离，
 * 以及默认规则对各数据范围的条件生成。
 */
class DataPermissionContextTest {

    @AfterEach
    void tearDown() {
        DataPermissionContext.clear();
    }

    // ---------- 默认值 ----------

    @Test
    void currentScope_defaultsToAllWhenUnset() {
        assertThat(DataPermissionContext.currentScope()).isEqualTo(DataScope.ALL);
    }

    @Test
    void currentTable_defaultsToEmptyWhenUnset() {
        assertThat(DataPermissionContext.currentTable()).isEmpty();
    }

    // ---------- 读写 ----------

    @Test
    void set_thenRead_returnsStoredValues() {
        DataPermissionContext.set(DataScope.DEPT, "t_order");

        assertThat(DataPermissionContext.currentScope()).isEqualTo(DataScope.DEPT);
        assertThat(DataPermissionContext.currentTable()).isEqualTo("t_order");
    }

    @Test
    void set_normalizesNullTableToEmptyString() {
        DataPermissionContext.set(DataScope.SELF, null);

        assertThat(DataPermissionContext.currentScope()).isEqualTo(DataScope.SELF);
        assertThat(DataPermissionContext.currentTable()).isEmpty();
    }

    @Test
    void set_overwritesPreviousValues() {
        DataPermissionContext.set(DataScope.SELF, "t_a");
        DataPermissionContext.set(DataScope.CUSTOM, "t_b");

        assertThat(DataPermissionContext.currentScope()).isEqualTo(DataScope.CUSTOM);
        assertThat(DataPermissionContext.currentTable()).isEqualTo("t_b");
    }

    @Test
    void clear_restoresDefaults() {
        DataPermissionContext.set(DataScope.DEPT_AND_CHILD, "t_order");
        DataPermissionContext.clear();

        assertThat(DataPermissionContext.currentScope()).isEqualTo(DataScope.ALL);
        assertThat(DataPermissionContext.currentTable()).isEmpty();
    }

    @Test
    void clear_isIdempotent() {
        DataPermissionContext.clear();
        DataPermissionContext.clear();

        assertThat(DataPermissionContext.currentScope()).isEqualTo(DataScope.ALL);
    }

    // ---------- 线程隔离 ----------

    @Test
    void context_isIsolatedPerThread() throws Exception {
        DataPermissionContext.set(DataScope.SELF, "main-table");

        AtomicReference<DataScope> otherScope = new AtomicReference<>();
        AtomicReference<String> otherTable = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        Thread worker = new Thread(() -> {
            try {
                otherScope.set(DataPermissionContext.currentScope());
                otherTable.set(DataPermissionContext.currentTable());
            } finally {
                done.countDown();
            }
        }, "dp-worker");
        worker.start();
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();

        assertThat(otherScope.get()).as("其他线程不应看到本线程的设置").isEqualTo(DataScope.ALL);
        assertThat(otherTable.get()).isEmpty();
        assertThat(DataPermissionContext.currentScope()).isEqualTo(DataScope.SELF);
    }

    @Test
    void set_inOtherThread_doesNotAffectCurrentThread() throws Exception {
        CountDownLatch done = new CountDownLatch(1);

        Thread worker = new Thread(() -> {
            try {
                DataPermissionContext.set(DataScope.CUSTOM, "worker-table");
            } finally {
                done.countDown();
            }
        }, "dp-worker-2");
        worker.start();
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();

        assertThat(DataPermissionContext.currentScope()).isEqualTo(DataScope.ALL);
        assertThat(DataPermissionContext.currentTable()).isEmpty();
    }

    // ---------- 默认规则 ----------

    @Test
    void defaultRule_returnsNullForAllScope() {
        assertThat(new DataPermissionRule.Default().condition(DataScope.ALL, "t_order")).isNull();
    }

    @Test
    void defaultRule_returnsEmptyConditionForRestrictedScopes() {
        DataPermissionRule rule = new DataPermissionRule.Default();

        assertThat(rule.condition(DataScope.SELF, "t_order")).isEmpty();
        assertThat(rule.condition(DataScope.DEPT, "t_order")).isEmpty();
        assertThat(rule.condition(DataScope.DEPT_AND_CHILD, "t_order")).isEmpty();
        assertThat(rule.condition(DataScope.CUSTOM, "t_order")).isEmpty();
    }

    @Test
    void defaultRule_ignoresTableName() {
        DataPermissionRule rule = new DataPermissionRule.Default();

        assertThat(rule.condition(DataScope.ALL, "")).isNull();
        assertThat(rule.condition(DataScope.SELF, "")).isEmpty();
    }

    @Test
    void defaultRule_implementsRuleContract() {
        assertThat(new DataPermissionRule.Default()).isInstanceOf(DataPermissionRule.class);
    }
}
