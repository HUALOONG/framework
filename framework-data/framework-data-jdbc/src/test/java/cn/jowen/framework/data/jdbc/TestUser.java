package cn.jowen.framework.data.jdbc;

import cn.jowen.framework.data.core.meta.GeneratedValue;
import cn.jowen.framework.data.core.meta.Id;
import cn.jowen.framework.data.core.meta.Table;

/**
 * 测试用 User 实体。
 */
@Table("app_user")
class User {

    @Id
    @GeneratedValue(GeneratedValue.Strategy.AUTO)
    private Long id;

    private String name;

    private Integer status;

    private String tenantId;

    public User() {
    }

    public User(String name, Integer status, String tenantId) {
        this.name = name;
        this.status = status;
        this.tenantId = tenantId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', status=" + status + ", tenantId='" + tenantId + "'}";
    }
}
