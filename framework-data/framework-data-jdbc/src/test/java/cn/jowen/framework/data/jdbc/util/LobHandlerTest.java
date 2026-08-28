package cn.jowen.framework.data.jdbc.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link LobHandler} CLOB/BLOB 读写测试。
 */
class LobHandlerTest {

    private final PreparedStatement stmt = mock(PreparedStatement.class);
    private final ResultSet rs = mock(ResultSet.class);

    @Test
    void setClob_null_writesNull() throws SQLException {
        LobHandler.setClob(stmt, 1, (String) null);
        verify(stmt).setNull(1, Types.CLOB);
    }

    @Test
    void setClob_string_writesStream() throws SQLException {
        LobHandler.setClob(stmt, 1, "hello");
        verify(stmt).setClob(anyInt(), any(StringReader.class), anyLong());
    }

    @Test
    void setClob_nullClob_writesNull() throws SQLException {
        LobHandler.setClob(stmt, 2, (Clob) null);
        verify(stmt).setNull(2, Types.CLOB);
    }

    @Test
    void setClob_clob_writesClob() throws SQLException {
        Clob clob = mock(Clob.class);
        LobHandler.setClob(stmt, 2, clob);
        verify(stmt).setClob(2, clob);
    }

    @Test
    void getStringAsClob_nullClob_returnsNull() throws SQLException {
        when(rs.getClob("note")).thenReturn(null);
        assertThat(LobHandler.getStringAsClob(rs, "note")).isNull();
    }

    @Test
    void getStringAsClob_readsContent() throws SQLException {
        Clob clob = mock(Clob.class);
        when(rs.getClob("note")).thenReturn(clob);
        when(clob.getCharacterStream()).thenReturn(new StringReader("hello world"));
        assertThat(LobHandler.getStringAsClob(rs, "note")).isEqualTo("hello world");
    }

    @Test
    void getStringAsClob_nullReader_returnsNull() throws SQLException {
        Clob clob = mock(Clob.class);
        when(rs.getClob("note")).thenReturn(clob);
        when(clob.getCharacterStream()).thenReturn(null);
        assertThat(LobHandler.getStringAsClob(rs, "note")).isNull();
    }

    @Test
    void getStringAsClob_readerThrows_wraps() throws SQLException {
        Clob clob = mock(Clob.class);
        when(rs.getClob("note")).thenReturn(clob);
        when(clob.getCharacterStream()).thenReturn(new StringReader("x") {
            @Override
            public int read(char[] cbuf, int off, int len) throws java.io.IOException {
                throw new java.io.IOException("boom");
            }
        });
        assertThatThrownBy(() -> LobHandler.getStringAsClob(rs, "note"))
                .isInstanceOf(SQLException.class);
    }

    @Test
    void setBlob_null_writesNull() throws SQLException {
        LobHandler.setBlob(stmt, 1, (byte[]) null);
        verify(stmt).setNull(1, Types.BLOB);
    }

    @Test
    void setBlob_bytes_writesStream() throws SQLException {
        LobHandler.setBlob(stmt, 1, new byte[]{1, 2, 3});
        verify(stmt).setBlob(anyInt(), any(ByteArrayInputStream.class), anyLong());
    }

    @Test
    void setBlob_nullBlob_writesNull() throws SQLException {
        LobHandler.setBlob(stmt, 2, (Blob) null);
        verify(stmt).setNull(2, Types.BLOB);
    }

    @Test
    void setBlob_blob_writesBlob() throws SQLException {
        Blob blob = mock(Blob.class);
        LobHandler.setBlob(stmt, 2, blob);
        verify(stmt).setBlob(2, blob);
    }

    @Test
    void getBytesAsBlob_nullBlob_returnsNull() throws SQLException {
        when(rs.getBlob("data")).thenReturn(null);
        assertThat(LobHandler.getBytesAsBlob(rs, "data")).isNull();
    }

    @Test
    void getBytesAsBlob_readsContent() throws SQLException {
        Blob blob = mock(Blob.class);
        when(rs.getBlob("data")).thenReturn(blob);
        when(blob.getBinaryStream()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        assertThat(LobHandler.getBytesAsBlob(rs, "data")).containsExactly(1, 2, 3);
    }

    @Test
    void getBytesAsBlob_nullStream_returnsNull() throws SQLException {
        Blob blob = mock(Blob.class);
        when(rs.getBlob("data")).thenReturn(blob);
        when(blob.getBinaryStream()).thenReturn(null);
        assertThat(LobHandler.getBytesAsBlob(rs, "data")).isNull();
    }

    @Test
    void getBytesAsBlob_streamThrows_wraps() throws SQLException {
        Blob blob = mock(Blob.class);
        when(rs.getBlob("data")).thenReturn(blob);
        when(blob.getBinaryStream()).thenReturn(new java.io.InputStream() {
            @Override
            public int read() throws java.io.IOException {
                throw new java.io.IOException("boom");
            }
        });
        assertThatThrownBy(() -> LobHandler.getBytesAsBlob(rs, "data"))
                .isInstanceOf(SQLException.class);
    }
}
