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
 * {@link DingTalkWebhookMessageSender} 具体实现契约：进程内 {@link HttpServer} 模拟钉钉回调端点，全程离线。
 */
class DingTalkWebhookMessageSenderTest {

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

    private static Message dingTalk(String receiver, String title, String content) {
        return Message.builder(MessageType.DINGTALK).receiver(receiver).title(title).content(content).build();
    }

    @Test
    void supportedTypeIsDingTalk() {
        assertThat(new DingTalkWebhookMessageSender().supportedType()).isEqualTo(MessageType.DINGTALK);
    }

    @Test
    void sendPostsDingTalkMarkdownJson() {
        HttpClient client = HttpClient.newHttpClient();
        DingTalkWebhookMessageSender sender = new DingTalkWebhookMessageSender(client);
        sender.send(dingTalk(baseUrl + "/robot", "报警", "CPU 过高"));

        String body = lastBody.get();
        assertThat(body).startsWith("{\"msgtype\":\"markdown\"");
        assertThat(body).contains("\"title\":\"报警\"");
        assertThat(body).contains("\"text\":\"CPU 过高\"");
        assertThat(lastContentType.get()).startsWith("application/json");
    }

    @Test
    void escapeDoubleQuote() {
        HttpClient client = HttpClient.newHttpClient();
        DingTalkWebhookMessageSender sender = new DingTalkWebhookMessageSender(client);
        sender.send(dingTalk(baseUrl + "/robot", "he\"llo", "world"));
        assertThat(lastBody.get()).contains("he\\\"llo");
    }

    @Test
    void escapeBackslash() {
        HttpClient client = HttpClient.newHttpClient();
        DingTalkWebhookMessageSender sender = new DingTalkWebhookMessageSender(client);
        sender.send(dingTalk(baseUrl + "/robot", "a\\b", "c"));
        assertThat(lastBody.get()).contains("a\\\\b");
    }

    @Test
    void escapeForwardSlash() {
        HttpClient client = HttpClient.newHttpClient();
        DingTalkWebhookMessageSender sender = new DingTalkWebhookMessageSender(client);
        sender.send(dingTalk(baseUrl + "/robot", "a/b", "c"));
        assertThat(lastBody.get()).contains("a\\/b");
    }

    @Test
    void escapeNewline() {
        HttpClient client = HttpClient.newHttpClient();
        DingTalkWebhookMessageSender sender = new DingTalkWebhookMessageSender(client);
        sender.send(dingTalk(baseUrl + "/robot", "line1\nline2", "c"));
        assertThat(lastBody.get()).contains("line1\\nline2");
    }

    @Test
    void escapeCarriageReturn() {
        HttpClient client = HttpClient.newHttpClient();
        DingTalkWebhookMessageSender sender = new DingTalkWebhookMessageSender(client);
        sender.send(dingTalk(baseUrl + "/robot", "a\rb", "c"));
        assertThat(lastBody.get()).contains("a\\rb");
    }

    @Test
    void escapeTab() {
        HttpClient client = HttpClient.newHttpClient();
        DingTalkWebhookMessageSender sender = new DingTalkWebhookMessageSender(client);
        sender.send(dingTalk(baseUrl + "/robot", "a\tb", "c"));
        assertThat(lastBody.get()).contains("a\\tb");
    }

    @Test
    void escapeBackspace() {
        HttpClient client = HttpClient.newHttpClient();
        DingTalkWebhookMessageSender sender = new DingTalkWebhookMessageSender(client);
        sender.send(dingTalk(baseUrl + "/robot", "a\bb", "c"));
        assertThat(lastBody.get()).contains("a\\bb");
    }

    @Test
    void escapeFormfeed() {
        HttpClient client = HttpClient.newHttpClient();
        DingTalkWebhookMessageSender sender = new DingTalkWebhookMessageSender(client);
        sender.send(dingTalk(baseUrl + "/robot", "a\fb", "c"));
        assertThat(lastBody.get()).contains("a\\fb");
    }

    @Test
    void escapeControlCharactersBelow0x20() {
        HttpClient client = HttpClient.newHttpClient();
        DingTalkWebhookMessageSender sender = new DingTalkWebhookMessageSender(client);
        String controlChars = new String(new char[]{(char) 0x01, (char) 0x02});
        sender.send(dingTalk(baseUrl + "/robot", controlChars, "test"));
        String body = lastBody.get();
        assertThat(body).contains("\\u0001");
        assertThat(body).contains("\\u0002");
    }

    @Test
    void non2xxResponseThrowsExtrasException() {
        lastStatus.set(500);
        HttpClient client = HttpClient.newHttpClient();
        DingTalkWebhookMessageSender sender = new DingTalkWebhookMessageSender(client);

        assertThatThrownBy(() -> sender.send(dingTalk(baseUrl + "/err", "t", "c")))
                .isInstanceOf(ExtrasException.class)
                .hasMessageContaining("状态码=500");
    }

    @Test
    void connectionFailureThrowsExtrasException() {
        HttpClient client = HttpClient.newHttpClient();
        DingTalkWebhookMessageSender sender = new DingTalkWebhookMessageSender(client);

        assertThatThrownBy(() -> sender.send(dingTalk("http://127.0.0.1:1/robot", "t", "c")))
                .isInstanceOf(ExtrasException.class)
                .hasMessageContaining("钉钉消息发送失败");
    }

    @Test
    void nullWebhookThrowsAssertException() {
        HttpClient client = HttpClient.newHttpClient();
        DingTalkWebhookMessageSender sender = new DingTalkWebhookMessageSender(client);

        assertThatThrownBy(() -> sender.send(dingTalk(null, "t", "c")))
                .isInstanceOf(ExtrasException.class)
                .hasMessageContaining("钉钉 webhook 地址不能为空");
    }

    @Test
    void nullHttpClientConstructorFallback() {
        DingTalkWebhookMessageSender sender = new DingTalkWebhookMessageSender(null);
        assertThatCode(() -> sender.send(dingTalk(baseUrl + "/robot", "t", "c")))
                .doesNotThrowAnyException();
    }

    @Test
    void nullTimeoutConstructorFallback() {
        HttpClient client = HttpClient.newHttpClient();
        DingTalkWebhookMessageSender sender = new DingTalkWebhookMessageSender(client, null);
        assertThatCode(() -> sender.send(dingTalk(baseUrl + "/robot", "t", "c")))
                .doesNotThrowAnyException();
    }

    @Test
    void defaultConstructorUsesDefaultClientAndTimeout() {
        DingTalkWebhookMessageSender sender = new DingTalkWebhookMessageSender();
        assertThatCode(() -> sender.send(dingTalk(baseUrl + "/robot", "t", "c")))
                .doesNotThrowAnyException();
    }
}
