package cn.jowen.framework.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 加解密 / 签名演示请求体。
 *
 * <p>配合 {@code @Encrypt(keyAlias = "default")} 使用时，客户端需先对该对象的密文进行
 * 加密后传输，框架在 {@code WebRequestBodyAdvice} 中自动解密为明文并绑定到该对象。</p>
  * @author Jowen
  * @since 0.0.1
 * @version 0.0.1
 */
public class PaymentRequest {

    /** 业务唯一标识，亦作为幂等键 / 签名参与字段 */
    private String bizId;

    /** 金额（演示字段，参与签名计算） */
    private java.math.BigDecimal amount;

    /** 备注 */
    private String remark;

    /**
     * 获取biz id。
     * @return 结果
     */
    public String getBizId() {
        return bizId;
    }

    /**
     * 设置biz id。
     * @param bizId 参数 bizId
     */
    public void setBizId(String bizId) {
        this.bizId = bizId;
    }

    /**
     * 获取amount。
     * @return 结果
     */
    public java.math.BigDecimal getAmount() {
        return amount;
    }

    /**
     * 设置amount。
     * @param amount 参数 amount
     */
    public void setAmount(java.math.BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * 获取remark。
     * @return 结果
     */
    public String getRemark() {
        return remark;
    }

    /**
     * 设置remark。
     * @param remark 参数 remark
     */
    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * 转换为string。
     * @return 结果
     */
    @Override
    public String toString() {
        return "PaymentRequest{bizId='" + bizId + "', amount=" + amount + ", remark='" + remark + "'}";
    }
}
