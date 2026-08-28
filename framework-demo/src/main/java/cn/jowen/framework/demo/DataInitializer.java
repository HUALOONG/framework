package cn.jowen.framework.demo;

import cn.jowen.framework.data.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 演示数据初始化：应用就绪后建表并插入示例数据。
 *
 * <p><b>为什么不用 schema.sql</b>：本示例使用框架自带的 {@link JdbcTemplate}，
 * 其数据源由 {@code JdbcContext} 自管，不走 Spring 的 {@code DataSource}，
 * 因此 Spring Boot 的 {@code spring.sql.init} 不会作用于它。
 * 在 {@code ApplicationRunner} 中用框架的 {@code execute} 建表是最直接可靠的方式。
 *
 * <p><b>扩展提示</b>：生产环境请改用 Flyway / Liquibase 等迁移工具管理 DDL。
 *
 * @author demo
  * @since 0.0.1
 * @version 0.0.1
 */
@Component
public class DataInitializer implements ApplicationRunner {

    /** log 常量。 */
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    /** jdbcTemplate 不可变字段。 */
    private final JdbcTemplate jdbcTemplate;

    /**
     * 构造实例。
     * @param jdbcTemplate 参数 jdbcTemplate
     */
    public DataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 执行run操作。
     * @param args 参数 args
     */
    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS demo_user (
                    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_name  VARCHAR(64)  NOT NULL,
                    email      VARCHAR(128),
                    phone      VARCHAR(32),
                    created_at TIMESTAMP
                )
                """);

        long count = countUsers();
        if (count == 0) {
            insertSeed("张三", "zhangsan@example.com", "13812345678");
            insertSeed("Alice", "alice@example.com", "13900001111");
            log.info("示例数据初始化完成，已插入 2 条用户记录");
        } else {
            log.info("用户表已存在 {} 条记录，跳过初始化", count);
        }
    }

    private long countUsers() {
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM demo_user", Long.class);
        return total == null ? 0L : total;
    }

    private void insertSeed(String username, String email, String phone) {
        jdbcTemplate.update(
                "INSERT INTO demo_user (user_name, email, phone, created_at) VALUES (?, ?, ?, ?)",
                username, email, phone, java.time.LocalDateTime.now());
    }
}
