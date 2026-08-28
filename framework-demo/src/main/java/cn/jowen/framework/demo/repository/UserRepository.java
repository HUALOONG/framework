package cn.jowen.framework.demo.repository;

import cn.jowen.framework.data.core.mapping.RowMapper;
import cn.jowen.framework.data.jdbc.core.JdbcTemplate;
import cn.jowen.framework.demo.entity.User;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户数据访问层：基于框架 {@link JdbcTemplate} 手写 SQL。
 *
 * <p><b>关键约定</b>：框架查询结果以 {@code Map<String, Object>} 行呈现，
 * 其键为<b>小写的数据库列名</b>（见 {@code JdbcUtils#resultSetToMaps}），
 * 故下方 {@code row.get("user_name")} 使用列名而非 Java 字段名。
 *
 * <p><b>扩展提示</b>：若希望免写 SQL，可改用框架的
 * {@code RepositoryFactory#getRepository(User.class)} 获得通用 CRUD 仓储。
 *
 * @author demo
  * @since 0.0.1
 * @version 0.0.1
 */
@Repository
public class UserRepository {

    /** 列名为小写，与框架行映射保持一致 */
    private static final RowMapper<User> USER_MAPPER = (row, rowNum) -> {
        User user = new User();
        user.setId(toLong(row.get("id")));
        user.setUsername((String) row.get("user_name"));
        user.setEmail((String) row.get("email"));
        user.setPhone((String) row.get("phone"));
        user.setCreatedAt(toLocalDateTime(row.get("created_at")));
        return user;
    };

    /**
     * 时间列转换：不同驱动可能返回 {@link LocalDateTime}、{@link java.sql.Timestamp}
     * 或字符串，此处统一归一化为 {@link LocalDateTime}。
     */
    private static LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime time) {
            return time;
        }
        if (value instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime();
        }
        if (value instanceof java.util.Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault());
        }
        if (value instanceof CharSequence text) {
            return LocalDateTime.parse(text);
        }
        return LocalDateTime.now();
    }

    /** jdbcTemplate 不可变字段。 */
    private final JdbcTemplate jdbcTemplate;

    /**
     * 构造实例。
     * @param jdbcTemplate 参数 jdbcTemplate
     */
    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

     /**
      * 获取find all。
      * @return 结果
      */
     * @return 结果
    /** 查询全部用户 */
    public List<User> findAll() {
        return jdbcTemplate.query(
                "SELECT id, user_name, email, phone, created_at FROM demo_user ORDER BY id",
                USER_MAPPER);
    }

     /**
      * 获取find by id。
      * @param id 参数 id
      * @return 结果
      */
     * @param id 参数 id
     * @return 结果
    /** 按主键查询，不存在返回 {@code null} */
    public User findById(Long id) {
        return jdbcTemplate.queryForObject(
                "SELECT id, user_name, email, phone, created_at FROM demo_user WHERE id = ?",
                USER_MAPPER, id);
    }

    /**
     * 新增用户。
     *
     * @param user 待新增用户
     * @return 数据库生成的主键，未能获取时为 {@code null}
     */
    public Long insert(User user) {
        Object key = jdbcTemplate.executeInsert(
                "INSERT INTO demo_user (user_name, email, phone, created_at) VALUES (?, ?, ?, ?)",
                user.getUsername(), user.getEmail(), user.getPhone(),
                user.getCreatedAt() == null ? LocalDateTime.now() : user.getCreatedAt());
        return toLong(key);
    }

     /**
      * 执行update操作。
      * @param user 参数 user
      * @return 结果
      */
     * @param user 参数 user
     * @return 结果
    /** 更新用户，返回受影响行数 */
    public int update(User user) {
        return jdbcTemplate.update(
                "UPDATE demo_user SET user_name = ?, email = ?, phone = ? WHERE id = ?",
                user.getUsername(), user.getEmail(), user.getPhone(), user.getId());
    }

     /**
      * 执行delete by id操作。
      * @param id 参数 id
      * @return 结果
      */
     * @param id 参数 id
     * @return 结果
    /** 按主键删除，返回受影响行数 */
    public int deleteById(Long id) {
        return jdbcTemplate.update("DELETE FROM demo_user WHERE id = ?", id);
    }

    /** 数值转换，入参为 {@code null} 时返回 {@code null} */
    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }
}
