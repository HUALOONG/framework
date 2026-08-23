package cn.jowen.framework.extras.notification.config;

import org.jspecify.annotations.NullMarked;

import java.util.Map;

/**
 * Email 通知渠道配置 POJO。
 *
 * <p>对应属性前缀 {@code framework.extras.notification.email}（由外部绑定，本轮不引入 @ConfigurationProperties）。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public class EmailProperties {

    /**
     * SMTP 服务器地址。
     */
    private String host;

    /**
     * SMTP 服务器端口（默认 587）。
     */
    private int port = 587;

    /**
     * 发件人邮箱地址。
     */
    private String from;

    /**
     * 登录用户名（通常与 from 相同）。
     */
    private String username;

    /**
     * 登录密码（或授权码）。
     */
    private String password;

    /**
     * 是否启用 SSL（true → 465 端口 SSL；false → 587 端口 STARTTLS）。
     */
    private boolean ssl;

    /**
     * 额外 JavaMail 属性（如 {@code mail.smtp.auth}）。
     */
    private Map<String, Object> properties;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public Map<String, Object> getProperties() {
        return properties == null ? Map.of() : properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}
