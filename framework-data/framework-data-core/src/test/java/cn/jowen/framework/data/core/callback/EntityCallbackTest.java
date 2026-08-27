package cn.jowen.framework.data.core.callback;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class EntityCallbackTest {

    @Test
    void defaultMethods_doNothing() {
        EntityCallback<String> callback = new EntityCallback<>() {};
        callback.beforeInsert("entity");
        callback.beforeUpdate("entity");
        callback.afterInsert("entity");
        callback.afterUpdate("entity");
    }

    @Test
    void overriddenMethods_invoked() {
        final int[] counter = {0};
        EntityCallback<String> callback = new EntityCallback<>() {
            @Override public void beforeInsert(String entity) { counter[0]++; }
            @Override public void beforeUpdate(String entity) { counter[0]++; }
            @Override public void afterInsert(String entity) { counter[0]++; }
            @Override public void afterUpdate(String entity) { counter[0]++; }
        };

        callback.beforeInsert("e");
        callback.beforeUpdate("e");
        callback.afterInsert("e");
        callback.afterUpdate("e");
        assertThat(counter[0]).isEqualTo(4);
    }
}
