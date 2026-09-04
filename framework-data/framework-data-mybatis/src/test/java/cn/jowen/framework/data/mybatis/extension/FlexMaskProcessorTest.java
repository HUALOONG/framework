package cn.jowen.framework.data.mybatis.extension;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FlexMaskProcessorTest {

    private final FlexMaskProcessor processor = new FlexMaskProcessor();

    @Test
    void maskMap_phone() {
        Map<String, String> map = Map.of("phone", "13812345678");
        Map<String, String> result = processor.maskMap(map, "PHONE");
        assertThat(result).isNotNull();
        assertThat(result.get("phone")).isNotNull();
    }

    @Test
    void maskMap_null() {
        assertThat(processor.maskMap(null, "PHONE")).isNull();
    }

    @Test
    void maskMap_empty() {
        assertThat(processor.maskMap(Map.of(), "PHONE")).isEmpty();
    }

    @Test
    void maskObject_noAnnotations() {
        Object obj = new Object();
        Object result = processor.maskObject(obj);
        assertThat(result).isSameAs(obj);
    }

    @Test
    void maskObject_null() {
        assertThat(processor.maskObject(null)).isNull();
    }

    @Test
    void maskList() {
        java.util.List<Object> list = java.util.List.of(new Object(), new Object());
        java.util.List<Object> result = processor.maskList(list);
        assertThat(result).hasSize(2);
    }

    @Test
    void maskList_emptyList_returnsSameInstance() {
        // 空集合无需逐个脱敏，直接返回原引用，避免无谓的列表分配
        java.util.List<Object> empty = java.util.List.of();
        assertThat(processor.maskList(empty)).isSameAs(empty);
    }
}
