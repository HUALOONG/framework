package cn.jowen.framework.data.mybatis.adapter;

import cn.jowen.framework.data.core.datasource.DataSource;
import cn.jowen.framework.data.core.datasource.DataSourceContext;
import cn.jowen.framework.data.core.datasource.PoolType;
import cn.jowen.framework.data.core.exception.DataAccessException;
import cn.jowen.framework.data.core.page.Page;
import cn.jowen.framework.data.core.page.Pageable;
import cn.jowen.framework.data.core.query.QueryWrapper;
import cn.jowen.framework.data.core.transaction.TransactionCallback;
import cn.jowen.framework.data.core.transaction.TransactionDefinition;
import cn.jowen.framework.data.core.transaction.TransactionStatus;
import cn.jowen.framework.data.core.transaction.TransactionTemplate;
import cn.jowen.framework.data.mybatis.extension.ExtensionRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link FlexDataSourceAdapter}、{@link FlexExceptionTranslator}、
 * {@link FlexTransactionAdapter}、{@link FlexRepositoryAdapter} 的单元测试。
 *
 * <p>这些适配器通过反射调用 MyBatis-Flex 的 BaseMapper / 动态数据源 / 事务管理器，
 * 且使用 {@code Class.getMethod}（仅返回 public 方法）。因此 fake 实现的方法必须声明为
 * {@code public}，才能被反射解析到，否则会触发 NullPointerException。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class FlexAdaptersTest {

    // ===================== Fake 实现（反射目标，方法必须 public） =====================

    public static final class FakeEntity {
        private final String id;
        public FakeEntity(String id) { this.id = id; }
        public String getId() { return id; }
    }

    public static final class FakeMapper {
        public final List<Object> store = new ArrayList<>();
        public Object selectById(Object id) {
            return store.stream().filter(e -> ((FakeEntity) e).getId().equals(id)).findFirst().orElse(null);
        }
        public List<Object> selectAll() { return new ArrayList<>(store); }
        public List<Object> selectList(QueryWrapper<?> w) { return new ArrayList<>(store); }
        public List<Object> selectList() { return new ArrayList<>(store); }
        public long selectCount(QueryWrapper<?> w) { return store.size(); }
        public long selectCount() { return store.size(); }
        public int insert(Object e) { store.add(e); return 1; }
        public int updateById(Object e) { return 1; }
        public int deleteById(Object id) {
            boolean removed = store.removeIf(e -> ((FakeEntity) e).getId().equals(id));
            return removed ? 1 : 0;
        }
        public int deleteAll() { int n = store.size(); store.clear(); return n; }
        public boolean existsById(Object id) {
            return store.stream().anyMatch(e -> ((FakeEntity) e).getId().equals(id));
        }
        public long count() { return store.size(); }
    }

    public static final class FakeDynamicDataSource {
        public String determineCurrentLookupKey() { return null; }
    }

    public static final class FakeRawTxManager {
        public boolean autoCommitSet;
        public boolean committed;
        public boolean rolledBack;
        public void setAutoCommit(boolean b) { this.autoCommitSet = true; }
        public void commit() { this.committed = true; }
        public void rollback() { this.rolledBack = true; }
    }

    public static final class FakeCoreDataSource implements DataSource {
        @Override public String getName() { return "fake"; }
        @Override public String getUrl() { return "jdbc:fake://localhost/test"; }
        @Override public String getUsername() { return "sa"; }
        @Override public String getPassword() { return null; }
        @Override public PoolType getPoolType() { return PoolType.HIKARI; }
    }

    // ===================== FlexDataSourceAdapter =====================

    @Test
    void dataSource_defaultWhenEmpty() {
        FlexDataSourceAdapter adapter = new FlexDataSourceAdapter(new FakeDynamicDataSource());
        assertThat(adapter.getDataSource()).isNotNull();
        assertThat(adapter.getDataSource("foo")).isNull();
        assertThat(adapter.getAllDataSources()).isEmpty();
    }

    @Test
    void dataSource_registerAndGet() {
        DataSource ds = new FakeCoreDataSource();
        FlexDataSourceAdapter adapter = new FlexDataSourceAdapter(new FakeDynamicDataSource());
        adapter.registerDataSource("primary", ds);
        assertThat(adapter.getDataSource("primary")).isSameAs(ds);
        assertThat(adapter.getAllDataSources()).containsExactly(ds);
    }

    @Test
    void dataSource_setAndClearCurrent() {
        FlexDataSourceAdapter adapter = new FlexDataSourceAdapter(new FakeDynamicDataSource());
        DataSourceContext.runWith("order", () -> {
            adapter.setCurrentDataSource("order");
            adapter.clearCurrentDataSource();
        });
    }

    @Test
    void dataSource_registerNull_throws() {
        FlexDataSourceAdapter adapter = new FlexDataSourceAdapter(new FakeDynamicDataSource());
        assertThatThrownBy(() -> adapter.registerDataSource(null, new FakeCoreDataSource()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.registerDataSource("name", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dataSource_nullDynamic_throws() {
        assertThatThrownBy(() -> new FlexDataSourceAdapter(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ===================== FlexExceptionTranslator =====================

    @Test
    void translator_singleton() {
        assertThat(FlexExceptionTranslator.getInstance()).isSameAs(FlexExceptionTranslator.getInstance());
    }

    @Test
    void translator_successReturnsNull() {
        DataAccessException result = FlexExceptionTranslator.getInstance()
                .translate("insert", (java.util.concurrent.Callable<Void>) () -> null);
        assertThat(result).isNull();
    }

    @Test
    void translator_failureIsConverted() {
        DataAccessException ex = FlexExceptionTranslator.getInstance()
                .translate("insert", (java.util.concurrent.Callable<Void>) () -> {
                    throw new RuntimeException("boom");
                });
        assertThat(ex).isInstanceOf(DataAccessException.class);
        assertThat(ex.getMessage()).contains("boom");
    }

    @Test
    void translator_staticTranslate() {
        DataAccessException ex = FlexExceptionTranslator.translate("selectById", FakeEntity.class, new RuntimeException("x"));
        assertThat(ex).isInstanceOf(DataAccessException.class);
        assertThat(ex.getMessage()).contains("x");
    }

    @Test
    void translator_nullArgs_throws() {
        java.util.concurrent.Callable<Void> task = () -> null;
        assertThatThrownBy(() -> FlexExceptionTranslator.getInstance().translate(null, task))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FlexExceptionTranslator.getInstance().translate("op", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FlexExceptionTranslator.translate(null, FakeEntity.class, new RuntimeException("x")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FlexExceptionTranslator.translate("op", FakeEntity.class, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ===================== FlexTransactionAdapter =====================

    @Test
    void transaction_templateAndLifecycle() {
        FakeRawTxManager raw = new FakeRawTxManager();
        FlexTransactionAdapter adapter = new FlexTransactionAdapter(raw);
        assertThat(adapter.template()).isInstanceOf(TransactionTemplate.class);

        TransactionStatus status = adapter.begin(TransactionDefinition.defaults());
        assertThat(status).isInstanceOf(TransactionStatus.class);
        adapter.commit(status);
        assertThat(raw.committed).isTrue();
        adapter.rollback(status);
        assertThat(raw.rolledBack).isTrue();
    }

    @Test
    void transaction_executeCallback_success() {
        FakeRawTxManager raw = new FakeRawTxManager();
        FlexTransactionAdapter adapter = new FlexTransactionAdapter(raw);
        AtomicBoolean ran = new AtomicBoolean(false);
        adapter.execute((TransactionCallback) status -> {
            ran.set(true);
            assertThat(status).isInstanceOf(TransactionStatus.class);
        }, "doWork");
        assertThat(ran).isTrue();
        assertThat(raw.committed).isTrue();
    }

    @Test
    void transaction_executeCallable_success() {
        FakeRawTxManager raw = new FakeRawTxManager();
        FlexTransactionAdapter adapter = new FlexTransactionAdapter(raw);
        String r = adapter.execute((java.util.concurrent.Callable<String>) () -> "done", "doWork");
        assertThat(r).isEqualTo("done");
        assertThat(raw.committed).isTrue();
    }

    @Test
    void transaction_executeCallback_rollbackOnException() {
        FakeRawTxManager raw = new FakeRawTxManager();
        FlexTransactionAdapter adapter = new FlexTransactionAdapter(raw);
        assertThatThrownBy(() -> adapter.execute((TransactionCallback) status -> {
            throw new IllegalStateException("fail");
        }, "doWork")).isInstanceOf(DataAccessException.class);
        assertThat(raw.rolledBack).isTrue();
    }

    @Test
    void transaction_executeCallable_rollbackOnException() {
        FakeRawTxManager raw = new FakeRawTxManager();
        FlexTransactionAdapter adapter = new FlexTransactionAdapter(raw);
        assertThatThrownBy(() -> adapter.execute((java.util.concurrent.Callable<String>) () -> {
            throw new IllegalStateException("fail");
        }, "doWork")).isInstanceOf(DataAccessException.class);
        assertThat(raw.rolledBack).isTrue();
    }

    @Test
    void transaction_nullArgs_throws() {
        FlexTransactionAdapter adapter = new FlexTransactionAdapter(new FakeRawTxManager());
        assertThatThrownBy(() -> adapter.execute((TransactionCallback) status -> { }, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.begin(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transaction_nullRaw_throws() {
        assertThatThrownBy(() -> new FlexTransactionAdapter(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ===================== FlexRepositoryAdapter =====================

    private FlexRepositoryAdapter<FakeEntity, String> newRepo(FakeMapper mapper) {
        return new FlexRepositoryAdapter<>(mapper, FakeEntity.class, ExtensionRegistry.defaults());
    }

    @Test
    void repo_constructorGuards() {
        FakeMapper mapper = new FakeMapper();
        assertThatThrownBy(() -> new FlexRepositoryAdapter<>(null, FakeEntity.class, ExtensionRegistry.defaults()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FlexRepositoryAdapter<>(mapper, null, ExtensionRegistry.defaults()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FlexRepositoryAdapter<>(mapper, FakeEntity.class, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void repo_selectById_andExists() {
        FakeMapper mapper = new FakeMapper();
        mapper.insert(new FakeEntity("1"));
        FlexRepositoryAdapter<FakeEntity, String> repo = newRepo(mapper);

        assertThat(repo.selectById("1")).isPresent();
        assertThat(repo.existsById("1")).isTrue();
        assertThat(repo.existsById("999")).isFalse();
        assertThat(repo.selectById("999")).isEmpty();
    }

    @Test
    void repo_selectAll_andCount() {
        FakeMapper mapper = new FakeMapper();
        mapper.insert(new FakeEntity("1"));
        mapper.insert(new FakeEntity("2"));
        FlexRepositoryAdapter<FakeEntity, String> repo = newRepo(mapper);

        assertThat(repo.selectAll()).hasSize(2);
        assertThat(repo.count()).isEqualTo(2);
        assertThat(repo.count(new QueryWrapper<FakeEntity>())).isEqualTo(2);
    }

    @Test
    void repo_selectPage() {
        FakeMapper mapper = new FakeMapper();
        mapper.insert(new FakeEntity("1"));
        mapper.insert(new FakeEntity("2"));
        FlexRepositoryAdapter<FakeEntity, String> repo = newRepo(mapper);

        Page<FakeEntity> page = repo.selectPage(Pageable.of(0, 10), null);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotal()).isEqualTo(2);
    }

    @Test
    void repo_insertOrUpdate_insertBranch() {
        FakeMapper mapper = new FakeMapper();
        FlexRepositoryAdapter<FakeEntity, String> repo = newRepo(mapper);

        FakeEntity e = new FakeEntity(null);
        repo.insertOrUpdate(e);
        assertThat(mapper.store).hasSize(1);
    }

    @Test
    void repo_insertOrUpdate_updateBranch() {
        FakeMapper mapper = new FakeMapper();
        FlexRepositoryAdapter<FakeEntity, String> repo = newRepo(mapper);

        mapper.insert(new FakeEntity("5"));
        repo.insertOrUpdate(new FakeEntity("5"));
        assertThat(mapper.store).hasSize(1);
    }

    @Test
    void repo_insertAll() {
        FakeMapper mapper = new FakeMapper();
        FlexRepositoryAdapter<FakeEntity, String> repo = newRepo(mapper);
        // id 为 null 的实体在 insertOrUpdate 中走 insert 分支
        repo.insertAll(List.of(new FakeEntity(null), new FakeEntity(null)));
        assertThat(mapper.store).hasSize(2);
    }

    @Test
    void repo_updateById() {
        FakeMapper mapper = new FakeMapper();
        FlexRepositoryAdapter<FakeEntity, String> repo = newRepo(mapper);
        mapper.insert(new FakeEntity("3"));
        assertThat(repo.updateById(new FakeEntity("3"))).isEqualTo(1);
    }

    @Test
    void repo_deleteById() {
        FakeMapper mapper = new FakeMapper();
        FlexRepositoryAdapter<FakeEntity, String> repo = newRepo(mapper);
        mapper.insert(new FakeEntity("4"));
        assertThat(repo.deleteById("4")).isEqualTo(1);
        assertThat(mapper.store).isEmpty();
    }

    @Test
    void repo_deleteEntity() {
        FakeMapper mapper = new FakeMapper();
        FlexRepositoryAdapter<FakeEntity, String> repo = newRepo(mapper);
        mapper.insert(new FakeEntity("8"));
        assertThat(repo.delete(new FakeEntity("8"))).isEqualTo(1);
    }

    @Test
    void repo_deleteAll() {
        FakeMapper mapper = new FakeMapper();
        FlexRepositoryAdapter<FakeEntity, String> repo = newRepo(mapper);
        mapper.insert(new FakeEntity("1"));
        assertThat(repo.deleteAll()).isEqualTo(1);
        assertThat(mapper.store).isEmpty();
    }
}
