package cn.jowen.framework.data.core.page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 测试 {@link Sort} 的不可变语义、工厂方法与 {@link Sort.Order} 的 SQL 渲染。
 */
class SortTest {

    @Test
    void unsortedIsEmptyAndHasNoOrders() {
        Sort sort = Sort.unsorted();
        assertThat(sort.isEmpty()).isTrue();
        assertThat(sort.getOrders()).isEmpty();
    }

    @Test
    void bySinglePropertyProducesAscOrder() {
        Sort sort = Sort.by("name");
        assertThat(sort.isEmpty()).isFalse();
        assertThat(sort.getOrders()).hasSize(1);

        Sort.Order order = sort.getOrders().get(0);
        assertThat(order.getProperty()).isEqualTo("name");
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void byPropertyWithDirectionProducesSpecifiedDirection() {
        Sort sort = Sort.by("createdAt", Sort.Direction.DESC);
        Sort.Order order = sort.getOrders().get(0);
        assertThat(order.getProperty()).isEqualTo("createdAt");
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void andAppendsOrdersAndKeepsOriginalsImmutable() {
        Sort first = Sort.by("a");
        Sort second = Sort.by("b", Sort.Direction.DESC);
        Sort merged = first.and(second);

        assertThat(merged.getOrders()).hasSize(2);
        assertThat(merged.getOrders().get(0).getProperty()).isEqualTo("a");
        assertThat(merged.getOrders().get(0).getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(merged.getOrders().get(1).getProperty()).isEqualTo("b");
        assertThat(merged.getOrders().get(1).getDirection()).isEqualTo(Sort.Direction.DESC);

        // 原 Sort 不被修改（不可变性）
        assertThat(first.getOrders()).hasSize(1);
        assertThat(second.getOrders()).hasSize(1);
    }

    @Test
    void orderToSqlRendersDirectionSuffix() {
        assertThat(new Sort.Order("prop", Sort.Direction.ASC).toSql()).isEqualTo("prop ASC");
        assertThat(new Sort.Order("prop", Sort.Direction.DESC).toSql()).isEqualTo("prop DESC");
    }

    @Test
    void getOrdersReturnsUnmodifiableList() {
        Sort sort = Sort.by("x").and(Sort.by("y", Sort.Direction.DESC));
        List<Sort.Order> orders = sort.getOrders();
        assertThatThrownBy(() -> orders.add(new Sort.Order("z", Sort.Direction.ASC)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void directionEnumContainsAscAndDesc() {
        assertThat(Sort.Direction.values())
                .containsExactly(Sort.Direction.ASC, Sort.Direction.DESC);
    }
}
