package cn.jowen.framework.data.core.page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 测试 {@link Page} 的 getter、总页数计算、首页/末页/空判定与内容不可变性。
 */
class PageTest {

    private static final List<String> CONTENT = List.of("a", "b", "c");

    @Test
    void gettersExposeConstructorValues() {
        Page<String> page = new Page<>(CONTENT, 100L, 2, 10);
        assertThat(page.getContent()).containsExactly("a", "b", "c");
        assertThat(page.getTotal()).isEqualTo(100L);
        assertThat(page.getPage()).isEqualTo(2);
        assertThat(page.getSize()).isEqualTo(10);
    }

    @Test
    void contentIsUnmodifiable() {
        Page<String> page = new Page<>(CONTENT, 3L, 0, 10);
        assertThatThrownBy(() -> page.getContent().add("d"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getTotalPagesHandlesZeroSize() {
        Page<String> page = new Page<>(CONTENT, 10L, 0, 0);
        assertThat(page.getTotalPages()).isZero();
    }

    @Test
    void getTotalPagesHandlesZeroTotal() {
        Page<String> page = new Page<>(CONTENT, 0L, 0, 10);
        assertThat(page.getTotalPages()).isZero();
    }

    @Test
    void getTotalPagesCeilsUp() {
        assertThat(new Page<>(CONTENT, 10L, 0, 3).getTotalPages()).isEqualTo(4);
        assertThat(new Page<>(CONTENT, 9L, 0, 3).getTotalPages()).isEqualTo(3);
        assertThat(new Page<>(CONTENT, 1L, 0, 10).getTotalPages()).isEqualTo(1);
    }

    @Test
    void isFirstWhenPageIsZero() {
        assertThat(new Page<>(CONTENT, 10L, 0, 3).isFirst()).isTrue();
        assertThat(new Page<>(CONTENT, 10L, 1, 3).isFirst()).isFalse();
    }

    @Test
    void isLastWhenBeyondTotalPages() {
        // total=10, size=3 -> 4 页（0..3），page=3 为末页。
        assertThat(new Page<>(CONTENT, 10L, 3, 3).isLast()).isTrue();
        assertThat(new Page<>(CONTENT, 10L, 1, 3).isLast()).isFalse();
    }

    @Test
    void isLastAlwaysTrueWhenNoPages() {
        assertThat(new Page<>(CONTENT, 0L, 0, 10).isLast()).isTrue();
        assertThat(new Page<>(CONTENT, 0L, 5, 10).isLast()).isTrue();
        assertThat(new Page<>(CONTENT, 10L, 0, 0).isLast()).isTrue();
    }

    @Test
    void isEmptyReflectsContent() {
        assertThat(new Page<>(CONTENT, 3L, 0, 10).isEmpty()).isFalse();
        assertThat(new Page<>(List.of(), 0L, 0, 10).isEmpty()).isTrue();
    }
}
