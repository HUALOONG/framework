package cn.jowen.framework.data.core.page;

import cn.jowen.framework.data.core.sort.Direction;
import cn.jowen.framework.data.core.sort.Sort;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class PageableTest {

    @Test
    void of_basic() {
        Pageable pageable = Pageable.of(0, 10);
        assertThat(pageable.getPage()).isEqualTo(0);
        assertThat(pageable.getSize()).isEqualTo(10);
        assertThat(pageable.getOffset()).isEqualTo(0);
        assertThat(pageable.getSort().isEmpty()).isTrue();
    }

    @Test
    void of_withSort() {
        Sort sort = Sort.by("name", Direction.DESC);
        Pageable pageable = Pageable.of(2, 5, sort);

        assertThat(pageable.getPage()).isEqualTo(2);
        assertThat(pageable.getSize()).isEqualTo(5);
        assertThat(pageable.getOffset()).isEqualTo(10);
        assertThat(pageable.getSort()).isSameAs(sort);
    }

    @Test
    void offset_calculation() {
        assertThat(Pageable.of(0, 20).getOffset()).isEqualTo(0);
        assertThat(Pageable.of(1, 20).getOffset()).isEqualTo(20);
        assertThat(Pageable.of(9, 10).getOffset()).isEqualTo(90);
    }

    @Test
    void invalid_negativePage() {
        assertThatThrownBy(() -> Pageable.of(-1, 10))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("页码不可为负");
    }

    @Test
    void invalid_zeroSize() {
        assertThatThrownBy(() -> Pageable.of(0, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("每页大小至少为 1");
    }

    @Test
    void invalid_negativeSize() {
        assertThatThrownBy(() -> Pageable.of(0, -5))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("每页大小至少为 1");
    }
}
