package cn.jowen.framework.data.core.datapermission;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DataPermissionContext} 测试：线程内设置/读取/覆盖/清理的生命周期，以及工具类私有构造约束。
 */
class DataPermissionContextTest {

    @AfterEach
    void tearDown() {
        DataPermissionContext.clear();
    }

    @Test
    void defaultState_scopeIsAllAndTableIsEmpty() {
        assertThat(DataPermissionContext.currentScope()).isEqualTo(DataScope.ALL);
        assertThat(DataPermissionContext.currentTable()).isEmpty();
        assertThat(DataPermissionContext.active()).isFalse();
    }

    @Test
    void set_bindsScopeAndTable() {
        DataPermissionContext.set(DataScope.SELF, "sys_user");

        assertThat(DataPermissionContext.currentScope()).isEqualTo(DataScope.SELF);
        assertThat(DataPermissionContext.currentTable()).isEqualTo("sys_user");
        assertThat(DataPermissionContext.active()).isTrue();
    }

    @Test
    void set_withNullTable_normalizesToEmpty() {
        DataPermissionContext.set(DataScope.DEPARTMENT, null);

        assertThat(DataPermissionContext.currentScope()).isEqualTo(DataScope.DEPARTMENT);
        assertThat(DataPermissionContext.currentTable()).isEmpty();
        assertThat(DataPermissionContext.active()).isTrue();
    }

    @Test
    void set_overwritesPreviousValue() {
        DataPermissionContext.set(DataScope.SELF, "t_user");
        DataPermissionContext.set(DataScope.CUSTOM, "t_order");

        assertThat(DataPermissionContext.currentScope()).isEqualTo(DataScope.CUSTOM);
        assertThat(DataPermissionContext.currentTable()).isEqualTo("t_order");
    }

    @Test
    void clear_resetsToDefault() {
        DataPermissionContext.set(DataScope.DEPARTMENT_AND_CHILD, "sys_dept");
        DataPermissionContext.clear();

        assertThat(DataPermissionContext.active()).isFalse();
        assertThat(DataPermissionContext.currentScope()).isEqualTo(DataScope.ALL);
        assertThat(DataPermissionContext.currentTable()).isEmpty();
    }

    @Test
    void clear_isIdempotent() {
        DataPermissionContext.clear();
        DataPermissionContext.clear();

        assertThat(DataPermissionContext.active()).isFalse();
    }

    @Test
    void contextIsThreadLocal_notSharedAcrossThreads() throws Exception {
        DataPermissionContext.set(DataScope.SELF, "sys_user");

        AtomicReference<DataScope> scopeInOther = new AtomicReference<>();
        AtomicReference<String> tableInOther = new AtomicReference<>();
        AtomicBoolean activeInOther = new AtomicBoolean(true);
        Thread other = new Thread(() -> {
            scopeInOther.set(DataPermissionContext.currentScope());
            tableInOther.set(DataPermissionContext.currentTable());
            activeInOther.set(DataPermissionContext.active());
        });
        other.start();
        other.join();

        assertThat(scopeInOther.get()).isEqualTo(DataScope.ALL);
        assertThat(tableInOther.get()).isEmpty();
        assertThat(activeInOther.get()).isFalse();
        // 子线程读取不影响主线程已设置的上下文
        assertThat(DataPermissionContext.currentScope()).isEqualTo(DataScope.SELF);
    }

    @Test
    void utilityClass_hasSinglePrivateConstructor() throws Exception {
        Constructor<?>[] ctors = DataPermissionContext.class.getDeclaredConstructors();
        assertThat(ctors).hasSize(1);
        assertThat(Modifier.isPrivate(ctors[0].getModifiers())).isTrue();

        ctors[0].setAccessible(true);
        assertThat(ctors[0].newInstance()).isInstanceOf(DataPermissionContext.class);
    }
}
