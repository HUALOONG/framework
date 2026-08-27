package cn.jowen.framework.data.core.page;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class SortTest {

    @Test
    void unsorted() {
        Sort sort = Sort.unsorted();
        assertThat(sort.getOrders()).isEmpty();
        assertThat(sort.isEmpty()).isTrue();
    }

    @Test
    void by_singleField_asc() {
        Sort sort = Sort.by("name");
        assertThat(sort.getOrders()).hasSize(1);
        assertThat(sort.getOrders().get(0).getProperty()).isEqualTo("name");
        assertThat(sort.getOrders().get(0).getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void by_singleField_desc() {
        Sort sort = Sort.by("name", Sort.Direction.DESC);
        assertThat(sort.getOrders().get(0).getProperty()).isEqualTo("name");
        assertThat(sort.getOrders().get(0).getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void and_merge() {
        Sort s1 = Sort.by("name", Sort.Direction.ASC);
        Sort s2 = Sort.by("age", Sort.Direction.DESC);
        Sort merged = s1.and(s2);

        assertThat(merged.getOrders()).hasSize(2);
        assertThat(merged.getOrders().get(0).getProperty()).isEqualTo("name");
        assertThat(merged.getOrders().get(0).getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(merged.getOrders().get(1).getProperty()).isEqualTo("age");
        assertThat(merged.getOrders().get(1).getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void order_toSql() {
        Sort.Order asc = new Sort.Order("name", Sort.Direction.ASC);
        assertThat(asc.toSql()).isEqualTo("name ASC");

        Sort.Order desc = new Sort.Order("name", Sort.Direction.DESC);
        assertThat(desc.toSql()).isEqualTo("name DESC");
    }

    @Test
    void isEmpty_afterAnd() {
        Sort s1 = Sort.by("name");
        Sort s2 = Sort.unsorted();
        assertThat(s1.and(s2).isEmpty()).isFalse();
    }
}
