package cn.jowen.framework.data.jdbc.util;

import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link ResultSetExtractor} 测试。
 */
class ResultSetExtractorTest {

    private final ResultSet rs = mock(ResultSet.class);

    @Test
    void scalar_noRow_returnsNull() throws SQLException {
        when(rs.next()).thenReturn(false);
        assertThat(ResultSetExtractor.scalar(rs, String.class)).isNull();
    }

    @Test
    void scalar_nullValue_returnsNull() throws SQLException {
        when(rs.next()).thenReturn(true);
        when(rs.getObject(1)).thenReturn(null);
        assertThat(ResultSetExtractor.scalar(rs, String.class)).isNull();
    }

    @Test
    void scalar_matchingType_returnsValue() throws SQLException {
        when(rs.next()).thenReturn(true);
        when(rs.getObject(1)).thenReturn(42);
        assertThat(ResultSetExtractor.scalar(rs, Integer.class)).isEqualTo(42);
    }

    @Test
    void scalar_convertibleType_returnsValue() throws SQLException {
        when(rs.next()).thenReturn(true);
        when(rs.getObject(1)).thenReturn(42L);
        assertThat(ResultSetExtractor.scalar(rs, Long.class)).isEqualTo(42L);
    }

    @Test
    void scalar_wrongType_throws() throws SQLException {
        when(rs.next()).thenReturn(true);
        when(rs.getObject(1)).thenReturn(42);
        assertThatThrownBy(() -> ResultSetExtractor.scalar(rs, String.class))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("无法将");
    }

    @Test
    void lambda_implementsExtract() throws SQLException {
        when(rs.next()).thenReturn(true, false);
        when(rs.getString(1)).thenReturn("a");
        ResultSetExtractor<String> extractor = r -> java.util.List.of(r.getString(1));
        assertThat(extractor.extract(rs)).containsExactly("a");
    }
}
