package cn.jowen.framework.data.core.page;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class PageTest {

    @Test
    void basic_page() {
        List<String> content = List.of("a", "b", "c");
        Page<String> page = new Page<>(content, 30L, 0, 10);

        assertThat(page.getContent()).containsExactly("a", "b", "c");
        assertThat(page.getTotal()).isEqualTo(30);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getPage()).isEqualTo(0);
        assertThat(page.getSize()).isEqualTo(10);
    }

    @Test
    void isFirst() {
        assertThat(new Page<>(List.of(), 0L, 0, 10).isFirst()).isTrue();
        assertThat(new Page<>(List.of(), 0L, 1, 10).isFirst()).isFalse();
    }

    @Test
    void isLast() {
        Page<String> first = new Page<>(List.of(), 30L, 0, 10);
        assertThat(first.isLast()).isFalse();

        Page<String> last = new Page<>(List.of(), 30L, 2, 10);
        assertThat(last.isLast()).isTrue();

        Page<String> single = new Page<>(List.of(), 5L, 0, 10);
        assertThat(single.isLast()).isTrue();
    }

    @Test
    void isEmpty() {
        assertThat(new Page<>(List.of(), 0L, 0, 10).isEmpty()).isTrue();
        assertThat(new Page<>(List.of("a"), 1L, 0, 10).isEmpty()).isFalse();
    }

    @Test
    void empty_page() {
        Page<String> page = Page.empty();
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotal()).isEqualTo(0);
        assertThat(page.getPage()).isEqualTo(0);
        assertThat(page.getSize()).isEqualTo(0);
        assertThat(page.getTotalPages()).isEqualTo(0);
        assertThat(page.isFirst()).isTrue();
        assertThat(page.isEmpty()).isTrue();
    }

    @Test
    void contentIsUnmodifiable() {
        List<String> source = List.of("a", "b");
        Page<String> page = new Page<>(source, 2L, 0, 10);
        assertThatThrownBy(() -> page.getContent().add("c"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void totalPages_rounding() {
        assertThat(new Page<>(List.of(), 10L, 0, 10).getTotalPages()).isEqualTo(1);
        assertThat(new Page<>(List.of(), 11L, 0, 10).getTotalPages()).isEqualTo(2);
        assertThat(new Page<>(List.of(), 0L, 0, 10).getTotalPages()).isEqualTo(0);
    }

    @Test
    void totalPages_withZeroSize() {
        assertThat(new Page<>(List.of(), 0L, 0, 0).getTotalPages()).isEqualTo(0);
    }
}
