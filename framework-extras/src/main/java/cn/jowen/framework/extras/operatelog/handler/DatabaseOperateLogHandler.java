package cn.jowen.framework.extras.operatelog.handler;

import cn.jowen.framework.extras.operatelog.OperateLogHandler;
import cn.jowen.framework.extras.operatelog.OperateLogRecord;
import org.jspecify.annotations.NullMarked;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * 数据库操作日志处理器：将 {@link OperateLogRecord} 落库到 {@code oper_log} 表。
 *
 * <p>零依赖：仅注入 JDK 标准 {@link DataSource}，用 {@link PreparedStatement} 写入。
 * 建表 DDL（列与 {@link OperateLogRecord} 字段一一对应）：
 * <pre>
 * CREATE TABLE oper_log (
 *     id BIGINT AUTO_INCREMENT PRIMARY KEY,
 *     trace_id VARCHAR(64), module VARCHAR(64), action VARCHAR(64),
 *     description VARCHAR(255), content VARCHAR(1024), operator VARCHAR(64), operator_id VARCHAR(64),
 *     status VARCHAR(16), error_message VARCHAR(1024), cost_time BIGINT,
 *     operate_time TIMESTAMP, extra VARCHAR(1024)
 * );
 * </pre>
 *
 * @author 王飞
 * @since 2026-08-27
 */
@NullMarked
public final class DatabaseOperateLogHandler implements OperateLogHandler {

    private static final String INSERT_SQL = """
            INSERT INTO oper_log (trace_id, module, action, description, content, operator, operator_id,
                                  status, error_message, cost_time, operate_time, extra)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final DataSource dataSource;

    /**
     * 构造处理器。
     *
     * @param dataSource 数据源，不可为 {@code null}
     */
    public DatabaseOperateLogHandler(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource must not be null");
        }
        this.dataSource = dataSource;
    }

    @Override
    public void handle(OperateLogRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("record must not be null");
        }
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(INSERT_SQL)) {
            ps.setString(1, record.traceId());
            ps.setString(2, record.module());
            ps.setString(3, record.action());
            ps.setString(4, record.description());
            ps.setString(5, record.content());
            ps.setString(6, record.operator());
            ps.setString(7, record.operatorId());
            ps.setString(8, record.status().name());
            ps.setString(9, record.errorMessage());
            ps.setLong(10, record.costTime());
            ps.setTimestamp(11, Timestamp.from(record.operateTime()));
            ps.setString(12, record.extra().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("操作日志落库失败", e);
        }
    }
}