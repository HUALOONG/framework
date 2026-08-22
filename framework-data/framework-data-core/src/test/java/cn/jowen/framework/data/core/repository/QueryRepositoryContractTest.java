package cn.jowen.framework.data.core.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import cn.jowen.framework.data.core.page.Page;
import cn.jowen.framework.data.core.page.Pageable;
import cn.jowen.framework.data.core.query.Query;

import org.junit.jupiter.api.Test;

/**
 * 接口契约测试：用轻量 fake 验证 {@link QueryRepository} 的按条件查询/分页/计数契约。
 */
class QueryRepositoryContractTest {

    record Item(String id, String name) {}

    static final class FakeQueryRepository<T> implements QueryRepository<T> {
        private final List<T> data;

        FakeQueryRepository(List<T> data) {
            this.data = new ArrayList<>(data);
        }

        @Override
        public List<T> findBy(Query query) {
            List<T> result = new ArrayList<>(data);
            if (query.getLimit() != null) {
                int end = Math.min(query.getLimit(), result.size());
                result = new ArrayList<>(result.subList(0, end));
            }
            return List.copyOf(result);
        }

        @Override
        public Page<T> findBy(Query query, Pageable pageable) {
            return new Page<>(List.copyOf(data), data.size(), pageable.getPage(), pageable.getSize());
        }

        @Override
        public long countBy(Query query) {
            return data.size();
        }
    }

    @Test
    void findByReturnsAllWhenNoLimit() {
        FakeQueryRepository<Item> r =
                new FakeQueryRepository<>(List.of(new Item("1", "a"), new Item("2", "b")));
        assertThat(r.findBy(Query.empty())).hasSize(2);
    }

    @Test
    void findByAppliesLimitFromQuery() {
        List<Item> items = List.of(new Item("1", "a"), new Item("2", "b"), new Item("3", "c"));
        FakeQueryRepository<Item> r = new FakeQueryRepository<>(items);
        List<Item> limited = r.findBy(Query.empty().limit(2));
        assertThat(limited).hasSize(2);
    }

    @Test
    void findByWithPageableReturnsPage() {
        FakeQueryRepository<Item> r =
                new FakeQueryRepository<>(List.of(new Item("1", "a")));
        Page<Item> page = r.findBy(Query.empty(), Pageable.of(0, 10));
        assertThat(page.getContent()).containsExactly(new Item("1", "a"));
        assertThat(page.getTotal()).isEqualTo(1L);
    }

    @Test
    void countByReturnsSize() {
        FakeQueryRepository<Item> r =
                new FakeQueryRepository<>(List.of(new Item("1", "a")));
        assertThat(r.countBy(Query.empty())).isEqualTo(1L);
    }
}
