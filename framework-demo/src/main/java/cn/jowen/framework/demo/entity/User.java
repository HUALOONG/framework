package cn.jowen.framework.demo.entity;

import cn.jowen.framework.data.core.meta.Column;
import cn.jowen.framework.data.core.meta.GeneratedValue;
import cn.jowen.framework.data.core.meta.Id;
import cn.jowen.framework.data.core.meta.Table;

import java.time.LocalDateTime;

/**
 * 示例实体：演示框架 ORM 映射注解的用法。
 *
 * <p>{@code @Table} 指定表名，{@code @Column} 指定列名（缺省按字段名驼峰转下划线），
 * {@code @GeneratedValue(AUTO)} 表示主键由数据库自增生成，插入时排除该列。
 *
 * <p><b>扩展提示</b>：切换主键策略为 {@code SNOWFLAKE}/{@code UUID} 即可由框架生成主键，
 * 无需改动数据库表结构。
 *
 * @author demo
  * @since 0.0.1
 * @version 0.0.1
 */
@Table("demo_user")
public class User {

    /** 主键，H2 自增列 */
    @Id
    @GeneratedValue(GeneratedValue.Strategy.AUTO)
    private Long id;

    /** 用户名 */
    @Column("user_name")
    private String username;

    /** 邮箱 */
    private String email;

    /** 手机号（日志脱敏演示字段） */
    private String phone;

    /** 创建时间 */
    @Column("created_at")
    private LocalDateTime createdAt;

    /**
     * 构造实例。
     */
    public User() {
    }

    /**
     * 构造实例。
     * @param username 参数 username
     * @param email 参数 email
     * @param phone 参数 phone
     */
    public User(String username, String email, String phone) {
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 获取id。
     * @return 结果
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置id。
     * @param id 参数 id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取username。
     * @return 结果
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置username。
     * @param username 参数 username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取email。
     * @return 结果
     */
    public String getEmail() {
        return email;
    }

    /**
     * 设置email。
     * @param email 参数 email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 获取phone。
     * @return 结果
     */
    public String getPhone() {
        return phone;
    }

    /**
     * 设置phone。
     * @param phone 参数 phone
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * 获取created at。
     * @return 结果
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置created at。
     * @param createdAt 参数 createdAt
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
