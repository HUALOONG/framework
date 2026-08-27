package cn.jowen.framework.data.core.mapping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TypeHandlerRegistryTest {

    @Test
    void registerAndGet_byType() {
        TypeHandlerRegistry registry = new TypeHandlerRegistry();
        TypeHandler<String> handler = new TypeHandler<>() {
            @Override public void setParameter(java.sql.PreparedStatement stmt, int index, String value, JdbcType jdbcType) throws Exception {}
            @Override public String getResult(java.sql.ResultSet rs, String columnName) throws Exception { return null; }
            @Override public String getResult(java.sql.ResultSet rs, int column) throws Exception { return null; }
        };

        assertThat(registry.get(String.class)).isNull();
        registry.register(String.class, handler);
        assertThat(registry.get(String.class)).isSameAs(handler);
    }

    @Test
    void get_byValue() {
        TypeHandlerRegistry registry = new TypeHandlerRegistry();
        TypeHandler<Integer> handler = new TypeHandler<>() {
            @Override public void setParameter(java.sql.PreparedStatement stmt, int index, Integer value, JdbcType jdbcType) throws Exception {}
            @Override public Integer getResult(java.sql.ResultSet rs, String columnName) throws Exception { return null; }
            @Override public Integer getResult(java.sql.ResultSet rs, int column) throws Exception { return null; }
        };
        registry.register(Integer.class, handler);
        assertThat(registry.get(42)).isSameAs(handler);
    }

    @Test
    void get_nullValue_returnsNull() {
        TypeHandlerRegistry registry = new TypeHandlerRegistry();
        assertThat(registry.get((Object) null)).isNull();
    }

    @Test
    void register_overwrite() {
        TypeHandlerRegistry registry = new TypeHandlerRegistry();
        TypeHandler<String> h1 = new TypeHandler<>() {
            @Override public void setParameter(java.sql.PreparedStatement stmt, int index, String value, JdbcType jdbcType) throws Exception {}
            @Override public String getResult(java.sql.ResultSet rs, String columnName) throws Exception { return "h1"; }
            @Override public String getResult(java.sql.ResultSet rs, int column) throws Exception { return "h1"; }
        };
        TypeHandler<String> h2 = new TypeHandler<>() {
            @Override public void setParameter(java.sql.PreparedStatement stmt, int index, String value, JdbcType jdbcType) throws Exception {}
            @Override public String getResult(java.sql.ResultSet rs, String columnName) throws Exception { return "h2"; }
            @Override public String getResult(java.sql.ResultSet rs, int column) throws Exception { return "h2"; }
        };

        registry.register(String.class, h1);
        assertThat(registry.get(String.class)).isSameAs(h1);

        registry.register(String.class, h2);
        assertThat(registry.get(String.class)).isSameAs(h2);
    }

    @Test
    void get_unregisteredType_returnsNull() {
        TypeHandlerRegistry registry = new TypeHandlerRegistry();
        assertThat(registry.get(Long.class)).isNull();
    }
}
