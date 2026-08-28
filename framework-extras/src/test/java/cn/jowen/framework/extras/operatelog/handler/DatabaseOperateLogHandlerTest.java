package cn.jowen.framework.extras.operatelog.handler;

import cn.jowen.framework.extras.operatelog.OperateLogRecord;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link DatabaseOperateLogHandler} 测试：经 H2 内嵌数据源验证落库。
 */
class DatabaseOperateLogHandlerTest {

    @Test
    void handle_insertsRecordIntoOperLog() throws Exception {
        DataSource dataSource = dataSource();
        DatabaseOperateLogHandler handler = new DatabaseOperateLogHandler(dataSource);

        handler.handle(OperateLogRecord.builder().module("order").action("create")
                .operator("joyin").operatorId("1").costTime(12L).build());

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT module, action, operator, operator_id, status, cost_time FROM oper_log");
             ResultSet rs = ps.executeQuery()) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("module")).isEqualTo("order");
            assertThat(rs.getString("action")).isEqualTo("create");
            assertThat(rs.getString("operator")).isEqualTo("joyin");
            assertThat(rs.getString("operator_id")).isEqualTo("1");
            assertThat(rs.getString("status")).isEqualTo("SUCCESS");
            assertThat(rs.getLong("cost_time")).isEqualTo(12L);
            assertThat(rs.next()).isFalse();
        }
    }

    @Test
    void nullArguments_throw() {
        assertThatThrownBy(() -> new DatabaseOperateLogHandler(null))
                .isInstanceOf(IllegalArgumentException.class);
        DatabaseOperateLogHandler handler = new DatabaseOperateLogHandler(dataSource());
        assertThatThrownBy(() -> handler.handle(null)).isInstanceOf(IllegalArgumentException.class);
    }

    private static DataSource dataSource() {
        org.h2.jdbcx.JdbcDataSource ds = new org.h2.jdbcx.JdbcDataSource();
        ds.setURL("jdbc:h2:mem:operlog;DB_CLOSE_DELAY=-1");
        try (Connection connection = ds.getConnection();
             Statement st = connection.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS oper_log (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        trace_id VARCHAR(64), module VARCHAR(64), action VARCHAR(64),
                        description VARCHAR(255), content VARCHAR(1024), operator VARCHAR(64), operator_id VARCHAR(64),
                        status VARCHAR(16), error_message VARCHAR(1024), cost_time BIGINT,
                        operate_time TIMESTAMP, extra VARCHAR(1024)
                    )
                    """);
        } catch (SQLException e) {
            throw new IllegalStateException("H2 建表失败", e);
        }
        return ds;
    }
}