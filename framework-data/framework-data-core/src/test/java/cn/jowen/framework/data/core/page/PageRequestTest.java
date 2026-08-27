package cn.jowen.framework.data.core.page;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class PageRequestTest {

    @Test
    void of_basic() {
        PageRequest request = PageRequest.of(1, 10);
        assertThat(request.getPage()).isEqualTo(1);
        assertThat(request.getSize()).isEqualTo(10);
        assertThat(request.getSort()).isNull();
    }

    @Test
    void toPageable_oneIndexed() {
        PageRequest request = PageRequest.of(1, 10);
        Pageable pageable = request.toPageable();

        assertThat(pageable.getPage()).isEqualTo(0);
        assertThat(pageable.getSize()).isEqualTo(10);
        assertThat(pageable.getOffset()).isEqualTo(0);
    }

    @Test
    void toPageable_secondPage() {
        PageRequest request = PageRequest.of(3, 20);
        Pageable pageable = request.toPageable();

        assertThat(pageable.getPage()).isEqualTo(2);
        assertThat(pageable.getSize()).isEqualTo(20);
        assertThat(pageable.getOffset()).isEqualTo(40);
    }

    @Test
    void toPageable_withSort() {
        Sort sort = Sort.by("name");
        PageRequest request = PageRequest.of(1, 10, sort);
        Pageable pageable = request.toPageable();

        assertThat(pageable.getSort()).isSameAs(sort);
    }

    @Test
    void invalid_zeroPage() {
        assertThatThrownBy(() -> PageRequest.of(0, 10))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("页码必须 >= 1");
    }

    @Test
    void invalid_negativePage() {
        assertThatThrownBy(() -> PageRequest.of(-1, 10))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("页码必须 >= 1");
    }

    @Test
    void invalid_zeroSize() {
        assertThatThrownBy(() -> PageRequest.of(1, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("每页大小必须 >= 1");
    }
}
