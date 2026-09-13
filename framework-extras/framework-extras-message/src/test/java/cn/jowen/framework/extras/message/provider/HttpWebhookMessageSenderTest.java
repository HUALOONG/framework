package cn.jowen.framework.extras.message.provider;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import cn.jowen.framework.extras.message.core.Message;
import cn.jowen.framework.extras.message.core.MessageType;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link HttpWebhookMessageSender} 具体实现契约：进程内 {@link HttpServer} 模拟回调端点，全程离线。
 */
class HttpWebhookMessageSenderTest {

    private HttpServer server;
    private String baseUrl;
    private HttpClient client;
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicReference<String> lastContentType = new AtomicReference<>();
    private final AtomicReference<Integer> lastStatus = new AtomicReference<>(200);

    @BeforeEach
    void setUp() throws IOException {
        lastBody.set(null);
        lastContentType.set(null);
        lastStatus.set(200);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/cb", exchange -> {
            try (InputStream in = exchange.getRequestBody()) {
                lastBody.set(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
            lastContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            byte[] resp = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(lastStatus.get(), resp.length);
            exchange.getResponseBody().write(resp);
            exchange.close();
        });
        server.createContext("/err", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();
        int port = server.getAddress().getPort();
        baseUrl = "http://127.0.0.1:" + port + "/cb";
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private static Message webhook(String receiver, String title, String content) {
        return Message.builder(MessageType.WEBHOOK).receiver(receiver).title(title).content(content).build();
    }

    @Test
    void supportedTypeIsWebhook() {
        assertThat(new HttpWebhookMessageSender().supportedType()).isEqualTo(MessageType.WEBHOOK);
    }

    @Test
    void sendPostsJsonEnvelopeByDefault() {
        HttpWebhookMessageSender sender = new HttpWebhookMessageSender(client);
        sender.send(webhook(baseUrl, "事件", "触发了"));

        assertThat(lastBody.get()).isEqualTo("{\"title\":\"事件\",\"content\":\"触发了\"}");
        assertThat(lastContentType.get()).startsWith("application/json");
    }

    @Test
    void sendRawContentModePostsVerbatim() {
        HttpWebhookMessageSender sender = new HttpWebhookMessageSender(client, true, Duration.ofSeconds(5));
        sender.send(webhook(baseUrl, "标题被忽略", "裸文本原文"));

        assertThat(lastBody.get()).isEqualTo("裸文本原文");
        assertThat(lastContentType.get()).startsWith("text/plain");
    }

    @Test
    void non2xxResponseThrowsExtrasException() {
        lastStatus.set(500);
        HttpWebhookMessageSender sender = new HttpWebhookMessageSender(client);
        String errUrl = baseUrl.replace("/cb", "/err");

        assertThatThrownBy(() -> sender.send(webhook(errUrl, "t", "c")))
                .isInstanceOf(ExtrasException.class)
                .hasMessageContaining("状态码=500");
    }

    @Test
    void connectionFailureThrowsExtrasException() {
        HttpWebhookMessageSender sender = new HttpWebhookMessageSender(client);
        // 端口 1 在本地基本恒为连接拒绝，且无需外网。
        assertThatThrownBy(() -> sender.send(webhook("http://127.0.0.1:1/hook", "t", "c")))
                .isInstanceOf(ExtrasException.class)
                .hasMessageContaining("Webhook 调用失败");
    }

    @Test
    void jsonEnvelopeEscapesSpecialChars() {
        HttpWebhookMessageSender sender = new HttpWebhookMessageSender(client);
        sender.send(webhook(baseUrl, "he\"llo", "li\nne"));

        String expected = "{\"title\":\"he\\\"llo\",\"content\":\"li\\nne\"}";
        assertThat(lastBody.get()).isEqualTo(expected);
    }

    @Test
    void escapeJsonHandlesCarriageReturnTabBackspaceFormfeed() {
        HttpWebhookMessageSender sender = new HttpWebhookMessageSender(client);
        sender.send(webhook(baseUrl, "\r\t\b\f", "\r\t\b\f"));

        String body = lastBody.get();
        assertThat(body).contains("\\r");
        assertThat(body).contains("\\t");
        assertThat(body).contains("\\b");
        assertThat(body).contains("\\f");
    }

    @Test
    void escapeJsonHandlesControlCharactersBelow0x20() {
        HttpWebhookMessageSender sender = new HttpWebhookMessageSender(client);
        // Use actual control chars via string concatenation to avoid escape issues
        String controlChars = new String(new char[]{(char)0x01, (char)0x02});
        sender.send(webhook(baseUrl, controlChars, "test"));

        String body = lastBody.get();
        assertThat(body).contains("\\u0001");
        assertThat(body).contains("\\u0002");
    }

    @Test
    void nullHttpClientConstructorFallback() {
        HttpWebhookMessageSender sender = new HttpWebhookMessageSender(null);
        assertThatCode(() -> sender.send(webhook(baseUrl, "t", "c"))).doesNotThrowAnyException();
    }

    @Test
    void nullTimeoutConstructorFallback() {
        HttpWebhookMessageSender sender = new HttpWebhookMessageSender(client, false, null);
        assertThatCode(() -> sender.send(webhook(baseUrl, "t", "c"))).doesNotThrowAnyException();
    }

    @Test
    void escapeBackslash() {
        HttpWebhookMessageSender sender = new HttpWebhookMessageSender(client);
        sender.send(webhook(baseUrl, "a\\b", "c"));
        assertThat(lastBody.get()).contains("a\\\\b");
    }

    @Test
    void escapeForwardSlash() {
        HttpWebhookMessageSender sender = new HttpWebhookMessageSender(client);
        sender.send(webhook(baseUrl, "a/b", "c"));
        assertThat(lastBody.get()).contains("a\\/b");
    }
}
