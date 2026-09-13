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
 * {@link WeComWebhookMessageSender} 具体实现契约：进程内 {@link HttpServer} 模拟企微回调端点，全程离线。
 */
class WeComWebhookMessageSenderTest {

    private HttpServer server;
    private String baseUrl;
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicReference<String> lastContentType = new AtomicReference<>();
    private final AtomicReference<Integer> lastStatus = new AtomicReference<>(200);

    @BeforeEach
    void setUp() throws IOException {
        lastBody.set(null);
        lastContentType.set(null);
        lastStatus.set(200);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/robot", exchange -> {
            try (InputStream in = exchange.getRequestBody()) {
                lastBody.set(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
            lastContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            byte[] resp = "{\"errcode\":0,\"errmsg\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
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
        baseUrl = "http://127.0.0.1:" + port;
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private static Message wecom(String receiver, String title, String content) {
        return Message.builder(MessageType.WECOM).receiver(receiver).title(title).content(content).build();
    }

    @Test
    void supportedTypeIsWeCom() {
        assertThat(new WeComWebhookMessageSender().supportedType()).isEqualTo(MessageType.WECOM);
    }

    @Test
    void sendPostsWeComMarkdownJson() {
        HttpClient client = HttpClient.newHttpClient();
        WeComWebhookMessageSender sender = new WeComWebhookMessageSender(client);
        sender.send(wecom(baseUrl + "/robot", "周报", "请及时提交"));

        String body = lastBody.get();
        assertThat(body).startsWith("{\"msgtype\":\"markdown\"");
        assertThat(body).contains("\"content\":\"请及时提交\"");
        // 企微 markdown 没有 title 字段在 JSON 体中
        assertThat(body).doesNotContain("\"title\"");
        assertThat(lastContentType.get()).startsWith("application/json");
    }

    @Test
    void sendEscapesSpecialCharsInJson() {
        HttpClient client = HttpClient.newHttpClient();
        WeComWebhookMessageSender sender = new WeComWebhookMessageSender(client);
        // 企微 markdown 只有 content 字段，title 不参与 JSON
        sender.send(wecom(baseUrl + "/robot", "标题被忽略", "he\"llo: li\nne"));

        String body = lastBody.get();
        assertThat(body).contains("he\\\"llo");
        assertThat(body).contains("li\\nne");
    }

    @Test
    void non2xxResponseThrowsExtrasException() {
        lastStatus.set(500);
        HttpClient client = HttpClient.newHttpClient();
        WeComWebhookMessageSender sender = new WeComWebhookMessageSender(client);
        String errUrl = baseUrl + "/err";

        assertThatThrownBy(() -> sender.send(wecom(errUrl, "t", "c")))
                .isInstanceOf(ExtrasException.class)
                .hasMessageContaining("状态码=500");
    }

    @Test
    void connectionFailureThrowsExtrasException() {
        HttpClient client = HttpClient.newHttpClient();
        WeComWebhookMessageSender sender = new WeComWebhookMessageSender(client);
        // 端口 1 在本地基本恒为连接拒绝，且无需外网。
        assertThatThrownBy(() -> sender.send(wecom("http://127.0.0.1:1/robot", "t", "c")))
                .isInstanceOf(ExtrasException.class)
                .hasMessageContaining("企业微信消息发送失败");
    }

    @Test
    void nullWebhookThrowsAssertException() {
        HttpClient client = HttpClient.newHttpClient();
        WeComWebhookMessageSender sender = new WeComWebhookMessageSender(client);

        assertThatThrownBy(() -> sender.send(wecom(null, "t", "c")))
                .isInstanceOf(ExtrasException.class)
                .hasMessageContaining("企业微信 webhook 地址不能为空");
    }

    @Test
    void customHttpClientIsUsed() {
        HttpClient client = HttpClient.newHttpClient();
        WeComWebhookMessageSender sender = new WeComWebhookMessageSender(client, Duration.ofSeconds(10));
        sender.send(wecom(baseUrl + "/robot", "t", "c"));

        // 如果用了自定义 client，请求应该成功
        assertThat(lastBody.get()).isNotNull();
    }

    @Test
    void escapeBackslash() {
        HttpClient client = HttpClient.newHttpClient();
        WeComWebhookMessageSender sender = new WeComWebhookMessageSender(client);
        sender.send(wecom(baseUrl + "/robot", "t", "a\\b"));
        assertThat(lastBody.get()).contains("a\\\\b");
    }

    @Test
    void escapeForwardSlash() {
        HttpClient client = HttpClient.newHttpClient();
        WeComWebhookMessageSender sender = new WeComWebhookMessageSender(client);
        sender.send(wecom(baseUrl + "/robot", "t", "a/b"));
        assertThat(lastBody.get()).contains("a\\/b");
    }

    @Test
    void escapeCarriageReturn() {
        HttpClient client = HttpClient.newHttpClient();
        WeComWebhookMessageSender sender = new WeComWebhookMessageSender(client);
        sender.send(wecom(baseUrl + "/robot", "t", "a\rb"));
        assertThat(lastBody.get()).contains("a\\rb");
    }

    @Test
    void escapeTab() {
        HttpClient client = HttpClient.newHttpClient();
        WeComWebhookMessageSender sender = new WeComWebhookMessageSender(client);
        sender.send(wecom(baseUrl + "/robot", "t", "a\tb"));
        assertThat(lastBody.get()).contains("a\\tb");
    }

    @Test
    void escapeBackspace() {
        HttpClient client = HttpClient.newHttpClient();
        WeComWebhookMessageSender sender = new WeComWebhookMessageSender(client);
        sender.send(wecom(baseUrl + "/robot", "t", "a\bb"));
        assertThat(lastBody.get()).contains("a\\bb");
    }

    @Test
    void escapeFormfeed() {
        HttpClient client = HttpClient.newHttpClient();
        WeComWebhookMessageSender sender = new WeComWebhookMessageSender(client);
        sender.send(wecom(baseUrl + "/robot", "t", "a\fb"));
        assertThat(lastBody.get()).contains("a\\fb");
    }

    @Test
    void escapeControlCharactersBelow0x20() {
        HttpClient client = HttpClient.newHttpClient();
        WeComWebhookMessageSender sender = new WeComWebhookMessageSender(client);
        String controlChars = new String(new char[]{(char) 0x01, (char) 0x02});
        sender.send(wecom(baseUrl + "/robot", "t", controlChars));
        String body = lastBody.get();
        assertThat(body).contains("\\u0001");
        assertThat(body).contains("\\u0002");
    }

    @Test
    void nullConstructorFallsBackToDefaults() {
        // 传入 null httpClient 与 null timeout，构造时应退化为默认客户端与默认超时；
        // 请求仍可成功发出，证明退化分支可用。
        WeComWebhookMessageSender sender = new WeComWebhookMessageSender(null, null);
        assertThatCode(() -> sender.send(wecom(baseUrl + "/robot", "t", "c")))
                .doesNotThrowAnyException();
        assertThat(lastBody.get()).isNotNull();
    }
}
