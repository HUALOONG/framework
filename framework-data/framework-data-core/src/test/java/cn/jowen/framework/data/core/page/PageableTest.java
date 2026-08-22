package cn.jowen.framework.data.core.page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * 测试 {@link Pageable} 的工厂方法、构造校验与偏移量计算。
 */
class PageableTest {

    @Test
    void ofWithoutSortUsesUnsorted() {
        Pageable pageable = Pageable.of(2, 10);
        assertThat(pageable.getPage()).isEqualTo(2);
        assertThat(pageable.getSize()).isEqualTo(10);
        assertThat(pageable.getSort().isEmpty()).isTrue();
    }

    @Test
    void ofWithSortUsesProvidedSort() {
        Sort sort = Sort.by("name", Sort.Direction.DESC);
        Pageable pageable = Pageable.of(0, 20, sort);
        assertThat(pageable.getSort()).isSameAs(sort);
    }

    @Test
    void negativePageRejected() {
        assertThatThrownBy(() -> Pageable.of(-1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("页码不可为负");
    }

    @Test
    void zeroSizeRejected() {
        assertThatThrownBy(() -> Pageable.of(0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("每页大小至少为 1");
    }

    @Test
    void getOffsetMultipliesPageAndSize() {
        assertThat(Pageable.of(0, 10).getOffset()).isZero();
        assertThat(Pageable.of(3, 25).getOffset()).isEqualTo(75L);
        assertThat(Pageable.of(2, 1).getOffset()).isEqualTo(2L);
    }
}
