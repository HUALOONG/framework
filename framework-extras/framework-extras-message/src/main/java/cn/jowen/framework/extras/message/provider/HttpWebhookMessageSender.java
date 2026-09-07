package cn.jowen.framework.extras.message.provider;

import cn.jowen.framework.extras.common.exception.ErrorCodeEnum;
import cn.jowen.framework.extras.common.exception.ExtrasException;
import cn.jowen.framework.extras.message.core.Message;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 基于 JDK 内置 {@link HttpClient} 的通用 Webhook 消息发送器（具体实现）。
 *
 * <p>作为 {@link WebhookMessageSender} 的即用实现，向 {@code receiver} 指定的回调地址发起
 * {@code HTTP POST}。零外部依赖（仅用 JDK 标准库），可直接实例化使用，也可注入自定义
 * {@link HttpClient} 以复用连接池或便于测试。</p>
 *
 * <p>负载形态：
 * <ul>
 *   <li>默认（{@code rawContent=false}）发送 JSON 信封 {@code {"title":"...","content":"..."}}，
 *       请求头 {@code Content-Type: application/json; charset=UTF-8}；</li>
 *   <li>{@code rawContent=true} 时直接以 {@code content} 原文作为请求体，
 *       请求头 {@code Content-Type: text/plain; charset=UTF-8}。</li>
 * </ul>
 * </p>
 *
 * <p>非 2xx 响应、I/O 失败或调用被中断时统一抛 {@link ExtrasException}，由上层
 * {@link cn.jowen.framework.extras.message.core.MessageService} 的重试逻辑接管。</p>
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class HttpWebhookMessageSender extends WebhookMessageSender {

    /** 默认连接/读取超时。 */
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    /** 复用的 HTTP 客户端。 */
    private final HttpClient httpClient;
    /** 是否以原文作为请求体（默认 false=JSON 信封）。 */
    private final boolean rawContent;
    /** 单次调用超时。 */
    private final Duration timeout;

    /**
     * 使用默认 {@link HttpClient} 与 JSON 信封模式构造。
     */
    public HttpWebhookMessageSender() {
        this(HttpClient.newHttpClient(), false, DEFAULT_TIMEOUT);
    }

    /**
     * 指定 {@link HttpClient}（便于复用或测试），其余取默认。
     *
     * @param httpClient 复用的 HTTP 客户端，为 {@code null} 时退化为默认客户端
     */
    public HttpWebhookMessageSender(HttpClient httpClient) {
        this(httpClient, false, DEFAULT_TIMEOUT);
    }

    /**
     * 全参构造。
     *
     * @param httpClient 复用的 HTTP 客户端，为 {@code null} 时退化为默认客户端
     * @param rawContent 是否以原文作为请求体（{@code true}=text/plain，{@code false}=JSON 信封）
     * @param timeout    单次调用超时，为 {@code null} 时取 {@link #DEFAULT_TIMEOUT}
     */
    public HttpWebhookMessageSender(HttpClient httpClient, boolean rawContent, Duration timeout) {
        this.httpClient = httpClient != null ? httpClient : HttpClient.newHttpClient();
        this.rawContent = rawContent;
        this.timeout = timeout != null ? timeout : DEFAULT_TIMEOUT;
    }

    @Override
    protected void doSend(String url, String title, String content) {
        String body = rawContent ? content : jsonEnvelope(title, content);
        String contentType = rawContent ? "text/plain; charset=UTF-8" : "application/json; charset=UTF-8";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout)
                .header("Content-Type", contentType)
                .header("User-Agent", "Jowen-Framework-WebhookSender")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ExtrasException("Webhook 调用失败: " + url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExtrasException("Webhook 调用被中断: " + url, e);
        }

        int status = response.statusCode();
        if (status < 200 || status > 299) {
            throw new ExtrasException(ErrorCodeEnum.INTERNAL_ERROR,
                    "Webhook 响应异常，状态码=" + status + ", url=" + url);
        }
    }

    /**
     * 构造最小化 JSON 信封并对字符串做转义（无第三方 JSON 依赖）。
     *
     * @param title   标题
     * @param content 内容
     * @return JSON 字符串
     */
    private static String jsonEnvelope(String title, String content) {
        return "{\"title\":" + escapeJson(title) + ",\"content\":" + escapeJson(content) + "}";
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
