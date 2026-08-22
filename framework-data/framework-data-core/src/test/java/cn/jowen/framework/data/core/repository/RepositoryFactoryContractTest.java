package cn.jowen.framework.data.core.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import cn.jowen.framework.data.core.page.Page;
import cn.jowen.framework.data.core.page.Pageable;

import org.junit.jupiter.api.Test;

/**
 * 接口契约测试：用轻量 fake 验证 {@link RepositoryFactory} 按类型产出仓储的契约。
 */
class RepositoryFactoryContractTest {

    record Widget(String id) {}

    static final class FakeRepository<T, ID> implements Repository<T, ID> {
        private final List<T> store = new ArrayList<>();

        @Override
        public Optional<T> findById(ID id) {
            return Optional.empty();
        }

        @Override
        public List<T> findAll() {
            return List.copyOf(store);
        }

        @Override
        public Page<T> findAll(Pageable pageable) {
            return new Page<>(List.of(), 0, pageable.getPage(), pageable.getSize());
        }

        @Override
        public T save(T entity) {
            store.add(entity);
            return entity;
        }

        @Override
        public List<T> saveAll(List<T> entities) {
            store.addAll(entities);
            return List.copyOf(entities);
        }

        @Override
        public int deleteById(ID id) {
            return 0;
        }

        @Override
        public boolean existsById(ID id) {
            return false;
        }

        @Override
        public long count() {
            return store.size();
        }
    }

    static final class FakeRepositoryFactory implements RepositoryFactory {
        @Override
        public <T, ID> Repository<T, ID> getRepository(Class<T> entityClass) {
            return new FakeRepository<>();
        }
    }

    @Test
    void getRepositoryReturnsNonNullRepository() {
        RepositoryFactory factory = new FakeRepositoryFactory();
        Repository<Widget, String> repo = factory.getRepository(Widget.class);
        assertThat(repo).isNotNull();
        assertThat(repo).isInstanceOf(Repository.class);
    }

    @Test
    void returnedRepositoryIsUsable() {
        RepositoryFactory factory = new FakeRepositoryFactory();
        Repository<Widget, String> repo = factory.getRepository(Widget.class);
        Widget w = repo.save(new Widget("1"));
        assertThat(w.id()).isEqualTo("1");
        assertThat(repo.count()).isEqualTo(1L);
    }
}
