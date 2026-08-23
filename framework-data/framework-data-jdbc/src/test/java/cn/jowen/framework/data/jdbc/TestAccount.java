package cn.jowen.framework.data.jdbc;

import cn.jowen.framework.data.core.meta.GeneratedValue;
import cn.jowen.framework.data.core.meta.Id;
import cn.jowen.framework.data.core.meta.Table;

import java.math.BigDecimal;

/**
 * 测试用 Account 实体。
 */
@Table("app_account")
class Account {

    @Id
    @GeneratedValue(GeneratedValue.Strategy.AUTO)
    private Long id;

    private String owner;

    private BigDecimal balance;

    public Account() {
    }

    public Account(String owner, BigDecimal balance) {
        this.owner = owner;
        this.balance = balance;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        return "Account{id=" + id + ", owner='" + owner + "', balance=" + balance + "}";
    }
}
