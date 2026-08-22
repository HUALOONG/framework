package cn.jowen.framework.data.core.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import cn.jowen.framework.data.core.page.Page;
import cn.jowen.framework.data.core.page.Pageable;

import org.junit.jupiter.api.Test;

/**
 * 接口契约测试：用内存版 {@link Repository} fake 验证 CRUD 形态自洽。
 */
class RepositoryContractTest {

    record Person(String id, String name) {}

    static final class FakeRepository<T, ID> implements Repository<T, ID> {
        private final Function<T, ID> idExtractor;
        private final Map<ID, T> store = new LinkedHashMap<>();

        FakeRepository(Function<T, ID> idExtractor) {
            this.idExtractor = idExtractor;
        }

        @Override
        public Optional<T> findById(ID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<T> findAll() {
            return List.copyOf(store.values());
        }

        @Override
        public Page<T> findAll(Pageable pageable) {
            List<T> all = List.copyOf(store.values());
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getSize(), all.size());
            List<T> slice = start >= all.size() ? List.of() : all.subList(start, end);
            return new Page<>(slice, all.size(), pageable.getPage(), pageable.getSize());
        }

        @Override
        public T save(T entity) {
            store.put(idExtractor.apply(entity), entity);
            return entity;
        }

        @Override
        public List<T> saveAll(List<T> entities) {
            for (T entity : entities) {
                store.put(idExtractor.apply(entity), entity);
            }
            return List.copyOf(entities);
        }

        @Override
        public int deleteById(ID id) {
            return store.remove(id) != null ? 1 : 0;
        }

        @Override
        public boolean existsById(ID id) {
            return store.containsKey(id);
        }

        @Override
        public long count() {
            return store.size();
        }
    }

    private FakeRepository<Person, String> repo() {
        return new FakeRepository<>((Person p) -> p.id());
    }

    @Test
    void saveReturnsEntityAndIsFindable() {
        FakeRepository<Person, String> r = repo();
        Person saved = r.save(new Person("1", "Alice"));
        assertThat(saved.id()).isEqualTo("1");
        assertThat(r.findById("1")).contains(new Person("1", "Alice"));
        assertThat(r.existsById("1")).isTrue();
        assertThat(r.count()).isEqualTo(1L);
    }

    @Test
    void saveAllReturnsListAndStoresAll() {
        FakeRepository<Person, String> r = repo();
        List<Person> toSave = List.of(new Person("1", "A"), new Person("2", "B"));
        List<Person> saved = r.saveAll(toSave);
        assertThat(saved).containsExactlyElementsOf(toSave);
        assertThat(r.count()).isEqualTo(2L);
        assertThat(r.findAll()).containsExactlyInAnyOrderElementsOf(toSave);
    }

    @Test
    void deleteByIdReturnsAffectedRows() {
        FakeRepository<Person, String> r = repo();
        r.save(new Person("1", "A"));
        assertThat(r.deleteById("1")).isEqualTo(1);
        assertThat(r.deleteById("missing")).isZero();
        assertThat(r.count()).isZero();
    }

    @Test
    void findAllPageableSlicesByOffsetAndSize() {
        FakeRepository<Person, String> r = repo();
        for (int i = 1; i <= 5; i++) {
            r.save(new Person(String.valueOf(i), "N" + i));
        }
        Page<Person> page = r.findAll(Pageable.of(1, 2));
        assertThat(page.getContent())
                .containsExactly(new Person("3", "N3"), new Person("4", "N4"));
        assertThat(page.getTotal()).isEqualTo(5L);
        assertThat(page.getPage()).isEqualTo(1);
    }

    @Test
    void findByIdMissingReturnsEmpty() {
        assertThat(repo().findById("x")).isEmpty();
    }
}
