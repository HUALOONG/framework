package cn.jowen.framework.data.core.repository;

import cn.jowen.framework.data.core.query.QueryWrapper;
import cn.jowen.framework.data.core.page.Page;
import cn.jowen.framework.data.core.page.Pageable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * {@link CrudRepository} 与 {@link PagingRepository} 测试。
 */
class CrudPagingRepositoryTest {

    static class TestCrudRepo implements CrudRepository<String, Long> {
        @Override public Optional<String> findById(Long id) { return Optional.empty(); }
        @Override public List<String> findAll() { return List.of(); }
        @Override public Page<String> findAll(Pageable pageable) { return Page.empty(); }
        @Override public String save(String entity) { return entity; }
        @Override public List<String> saveAll(List<String> entities) { return entities; }
        @Override public int deleteById(Long id) { return 0; }
        @Override public boolean existsById(Long id) { return false; }
        @Override public long count() { return 0; }
        @Override public String update(String entity) { return entity; }
        @Override public int delete(String entity) { return 1; }
        @Override public int deleteAll() { return 0; }
    }

    @Test
    void crudRepository_saveReturnsEntity() {
        TestCrudRepo repo = new TestCrudRepo();
        String result = repo.save("entity");
        assertThat(result).isEqualTo("entity");
    }

    @Test
    void crudRepository_updateReturnsEntity() {
        TestCrudRepo repo = new TestCrudRepo();
        String result = repo.update("entity");
        assertThat(result).isEqualTo("entity");
    }

    @Test
    void crudRepository_deleteReturnsOne() {
        TestCrudRepo repo = new TestCrudRepo();
        assertThat(repo.delete("entity")).isEqualTo(1);
    }

    @Test
    void pagingRepository_page_noWrapper() {
        PagingRepository<String, Long> repo = new PagingRepository<>() {
            @Override public Optional<String> findById(Long id) { return Optional.empty(); }
            @Override public List<String> findAll() { return List.of(); }
            @Override public Page<String> findAll(Pageable pageable) { return Page.empty(); }
            @Override public String save(String entity) { return entity; }
            @Override public List<String> saveAll(List<String> entities) { return entities; }
            @Override public int deleteById(Long id) { return 0; }
            @Override public boolean existsById(Long id) { return false; }
            @Override public long count() { return 0; }
            @Override public String update(String entity) { return entity; }
            @Override public int delete(String entity) { return 1; }
            @Override public int deleteAll() { return 0; }
            @Override public Page<String> page(Pageable pageable) { return Page.empty(); }
            @Override public Page<String> page(Pageable pageable, QueryWrapper<String> wrapper) { return Page.empty(); }
        };
        Page<String> result = repo.page(Pageable.of(0, 10));
        assertThat(result).isNotNull();
    }

    @Test
    void pagingRepository_page_withWrapper() {
        PagingRepository<String, Long> repo = new PagingRepository<>() {
            @Override public Optional<String> findById(Long id) { return Optional.empty(); }
            @Override public List<String> findAll() { return List.of(); }
            @Override public Page<String> findAll(Pageable pageable) { return Page.empty(); }
            @Override public String save(String entity) { return entity; }
            @Override public List<String> saveAll(List<String> entities) { return entities; }
            @Override public int deleteById(Long id) { return 0; }
            @Override public boolean existsById(Long id) { return false; }
            @Override public long count() { return 0; }
            @Override public String update(String entity) { return entity; }
            @Override public int delete(String entity) { return 1; }
            @Override public int deleteAll() { return 0; }
            @Override public Page<String> page(Pageable pageable) { return Page.empty(); }
            @Override public Page<String> page(Pageable pageable, QueryWrapper<String> wrapper) { return Page.empty(); }
        };
        Page<String> result = repo.page(Pageable.of(0, 10), new QueryWrapper<>());
        assertThat(result).isNotNull();
    }
}
