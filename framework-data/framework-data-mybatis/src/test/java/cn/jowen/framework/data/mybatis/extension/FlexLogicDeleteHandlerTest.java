package cn.jowen.framework.data.mybatis.extension;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlexLogicDeleteHandlerTest {

    @Test
    void toLogicDelete_setsDeletedField() {
        FlexLogicDeleteHandler handler = new FlexLogicDeleteHandler("deleted", 1);
        DeletableEntity entity = new DeletableEntity();
        assertThat(handler.toLogicDelete(entity)).isTrue();
        assertThat(entity.getDeleted()).isEqualTo(1);
    }

    @Test
    void toLogicDelete_missingField_returnsFalse() {
        FlexLogicDeleteHandler handler = new FlexLogicDeleteHandler("deleted", 1);
        assertThat(handler.toLogicDelete(new Object())).isFalse();
    }

    @Test
    void deleteCondition() {
        FlexLogicDeleteHandler handler = new FlexLogicDeleteHandler();
        assertThat(handler.deleteCondition()).isEqualTo("deleted = 0");
    }

    @Test
    void deleteCondition_customField() {
        FlexLogicDeleteHandler handler = new FlexLogicDeleteHandler("is_removed", "true");
        assertThat(handler.deleteCondition()).isEqualTo("is_removed = 0");
    }

    static class DeletableEntity {
        Integer deleted;
        Integer getDeleted() { return deleted; }
        void setDeleted(Integer v) { this.deleted = v; }
    }
}
