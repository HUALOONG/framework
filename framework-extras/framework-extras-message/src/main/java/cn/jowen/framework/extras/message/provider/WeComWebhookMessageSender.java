package cn.jowen.framework.extras.message.provider;

import cn.jowen.framework.extras.common.exception.ErrorCodeEnum;
import cn.jowen.framework.extras.common.exception.ExtrasException;
import cn.jowen.framework.extras.common.util.Assert;
import cn.jowen.framework.extras.message.core.Message;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 企业微信群机器人消息发送器（具体实现）。
 *
 * <p>基于 JDK 内置 {@link HttpClient}，按企业微信机器人 webhook 协议发送 Markdown 消息。
 * 零外部依赖，可直接实例化使用，也可注入自定义 {@link HttpClient} 以复用连接池或便于测试。</p>
 *
 * <p>企业微信机器人 Markdown 消息格式：
 * <pre>
 * {
 *   "msgtype": "markdown",
 *   "markdown": {"content": "{content}"}
 * }
 * </pre>
 * 注意：企业微信机器人不支持 title 字段，title 仅用于日志。
 * </p>
 *
 * <p>非 2xx 响应、I/O 失败或调用被中断时统一抛 {@link ExtrasException}。</p>
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class WeComWebhookMessageSender extends WeComMessageSender {

    /** 默认超时。 */
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    /** 复用的 HTTP 客户端。 */
    private final HttpClient httpClient;
    /** 单次调用超时。 */
    private final Duration timeout;

    /**
     * 使用默认 {@link HttpClient} 构造。
     */
    public WeComWebhookMessageSender() {
        this(HttpClient.newHttpClient(), DEFAULT_TIMEOUT);
    }

    /**
     * 指定 {@link HttpClient}（便于复用或测试），超时取默认。
     *
     * @param httpClient 复用的 HTTP 客户端，为 {@code null} 时退化为默认客户端
     */
    public WeComWebhookMessageSender(HttpClient httpClient) {
        this(httpClient, DEFAULT_TIMEOUT);
    }

    /**
     * 全参构造。
     *
     * @param httpClient 复用的 HTTP 客户端，为 {@code null} 时退化为默认客户端
     * @param timeout    单次调用超时，为 {@code null} 时取 {@link #DEFAULT_TIMEOUT}
     */
    public WeComWebhookMessageSender(HttpClient httpClient, Duration timeout) {
        this.httpClient = httpClient != null ? httpClient : HttpClient.newHttpClient();
        this.timeout = timeout != null ? timeout : DEFAULT_TIMEOUT;
    }

    @Override
    protected void doSend(String webhook, String title, String content) {
        Assert.hasText(webhook, "企业微信 webhook 地址不能为空");
        String body = weComMarkdownJson(content);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhook))
                .timeout(timeout)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("User-Agent", "Jowen-Framework-WeComSender")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ExtrasException("企业微信消息发送失败: " + webhook, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExtrasException("企业微信消息发送被中断: " + webhook, e);
        }

        int status = response.statusCode();
        if (status < 200 || status > 299) {
            throw new ExtrasException(ErrorCodeEnum.INTERNAL_ERROR,
                    "企业微信机器人响应异常，状态码=" + status + ", webhook=" + webhook);
        }
    }

    /**
     * 构造企业微信机器人 Markdown 消息 JSON 体。
     *
     * @param content 内容
     * @return JSON 字符串
     */
    private static String weComMarkdownJson(String content) {
        return "{\"msgtype\":\"markdown\","
                + "\"markdown\":{\"content\":" + escapeJson(content)
                + "}}";
    }

    /**
     * 按 RFC 8259 对 JSON 字符串做最小转义。
     *
     * @param value 原始字符串（非 {@code null}）
     * @return 转义后的双引号包裹片段
     */
    private static String escapeJson(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 8);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '/' -> sb.append("\\/");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
