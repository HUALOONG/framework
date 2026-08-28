package cn.jowen.framework.data.jdbc.statement;

import cn.jowen.framework.data.core.mapping.TypeHandler;
import cn.jowen.framework.data.core.mapping.TypeHandlerRegistry;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ParameterBinder} 与 {@link BatchParameterBinder} 测试。
 */
class ParameterBinderTest {

    private final PreparedStatement stmt = mock(PreparedStatement.class);
    private final TypeHandlerRegistry registry = mock(TypeHandlerRegistry.class);

    @Test
    void bind_nullValue_setNull() throws SQLException {
        ParameterBinder.bind(stmt, 1, null, registry);
        verify(stmt).setNull(1, java.sql.Types.NULL);
    }

    @Test
    void bind_noHandler_setObject() throws SQLException {
        when(registry.get("abc")).thenReturn(null);
        ParameterBinder.bind(stmt, 2, "abc", registry);
        verify(stmt).setObject(2, "abc");
    }

    @Test
    void bind_withHandler_delegates() throws Exception {
        TypeHandler<Object> handler = typedHandler();
        doReturn(handler).when(registry).get("abc");
        ParameterBinder.bind(stmt, 1, "abc", registry);
        verify(handler).setParameter(eq(stmt), eq(1), eq("abc"), any());
    }

    @Test
    void bind_handlerThrows_wrapsSqlException() throws Exception {
        TypeHandler<Object> handler = typedHandler();
        doReturn(handler).when(registry).get("abc");
        doThrow(new IllegalStateException("bad handler")).when(handler)
                .setParameter(eq(stmt), eq(1), eq("abc"), any());

        assertThatThrownBy(() -> ParameterBinder.bind(stmt, 1, "abc", registry))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("参数绑定失败");
    }

    @SuppressWarnings("unchecked")
    private static TypeHandler<Object> typedHandler() {
        return (TypeHandler<Object>) mock(TypeHandler.class);
    }

    @Test
    void bindAll_bindsAllParams() throws SQLException {
        when(registry.get("a")).thenReturn(null);
        when(registry.get(1)).thenReturn(null);
        ParameterBinder.bindAll(stmt, List.of("a", 1), registry);
        verify(stmt).setObject(1, "a");
        verify(stmt).setObject(2, 1);
    }

    @Test
    void addBatches_addsEachBatch() throws SQLException {
        when(registry.get("a")).thenReturn(null);
        when(registry.get("b")).thenReturn(null);
        BatchParameterBinder.addBatches(stmt, List.of(new Object[]{"a"}, new Object[]{"b"}), registry);
        verify(stmt, times(2)).addBatch();
    }

    @Test
    void addBatchesAsLists_addsEachBatch() throws SQLException {
        when(registry.get("x")).thenReturn(null);
        BatchParameterBinder.addBatchesAsLists(stmt, List.of(List.of("x")), registry);
        verify(stmt).addBatch();
        verify(stmt, never()).executeBatch();
    }
}
