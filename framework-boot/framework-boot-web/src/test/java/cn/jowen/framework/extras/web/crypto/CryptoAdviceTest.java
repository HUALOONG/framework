package cn.jowen.framework.extras.web.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CryptoAdviceTest {

    @Mock
    private CryptoProcessor processor;
    @Mock
    private MethodParameter parameter;
    @Mock
    private HttpInputMessage inputMessage;
    @Mock
    private HttpHeaders headers;
    @Mock
    private Encrypt encrypt;
    @Mock
    private ServerHttpRequest serverRequest;
    @Mock
    private ServerHttpResponse serverResponse;

    @BeforeEach
    void setUp() {
        doReturn(CryptoAdviceTest.class).when(parameter).getContainingClass();
        when(encrypt.keyAlias()).thenReturn("default");
    }

    private CryptoAdvice advice() {
        return new CryptoAdvice(processor);
    }

    @Test
    void supportsRequestBody_withMethodAnnotation_returnsTrue() {
        when(parameter.getMethodAnnotation(Encrypt.class)).thenReturn(encrypt);

        assertThat(advice().supports(parameter, String.class, null)).isTrue();
    }

    @Test
    void supportsRequestBody_withClassAnnotation_returnsTrue() {
        when(parameter.getMethodAnnotation(Encrypt.class)).thenReturn(null);
        doReturn(EncryptedController.class).when(parameter).getContainingClass();

        assertThat(advice().supports(parameter, String.class, null)).isTrue();
    }

    @Test
    void supportsRequestBody_withoutAnnotation_returnsFalse() {
        when(parameter.getMethodAnnotation(Encrypt.class)).thenReturn(null);

        assertThat(advice().supports(parameter, String.class, null)).isFalse();
    }

    @Test
    void beforeBodyRead_decryptsAndReturnsNewMessage() throws IOException {
        when(parameter.getMethodAnnotation(Encrypt.class)).thenReturn(encrypt);
        when(inputMessage.getBody())
                .thenReturn(new ByteArrayInputStream("cipher".getBytes(StandardCharsets.UTF_8)));
        when(inputMessage.getHeaders()).thenReturn(headers);
        when(processor.decrypt("cipher", "default")).thenReturn("plain");

        HttpInputMessage out = advice().beforeBodyRead(inputMessage, parameter, String.class, null);

        assertThat(new String(out.getBody().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("plain");
        assertThat(out.getHeaders()).isSameAs(headers);
    }

    @Test
    void beforeBodyRead_nullInput_returnsNull() throws IOException {
        when(parameter.getMethodAnnotation(Encrypt.class)).thenReturn(encrypt);

        assertThat(advice().beforeBodyRead(null, parameter, String.class, null)).isNull();
    }

    @Test
    void beforeBodyRead_withoutAlias_returnsSameMessage() throws IOException {
        when(parameter.getMethodAnnotation(Encrypt.class)).thenReturn(null);

        HttpInputMessage out = advice().beforeBodyRead(inputMessage, parameter, String.class, null);

        assertThat(out).isSameAs(inputMessage);
    }

    @Test
    void afterBodyRead_returnsBody() {
        Object body = new Object();

        assertThat(advice().afterBodyRead(body, inputMessage, parameter, String.class, null)).isSameAs(body);
    }

    @Test
    void supportsResponse_withAnnotation_returnsTrue() {
        when(parameter.getMethodAnnotation(Encrypt.class)).thenReturn(encrypt);

        assertThat(advice().supports(parameter, null)).isTrue();
    }

    @Test
    void beforeBodyWrite_string_encrypts() {
        when(parameter.getMethodAnnotation(Encrypt.class)).thenReturn(encrypt);
        when(processor.encrypt("hello", "default")).thenReturn("cipher");

        Object out = advice().beforeBodyWrite("hello", parameter, MediaType.APPLICATION_JSON,
                null, serverRequest, serverResponse);

        assertThat(out).isEqualTo("cipher");
    }

    @Test
    void beforeBodyWrite_byteArray_encryptsBytes() {
        when(parameter.getMethodAnnotation(Encrypt.class)).thenReturn(encrypt);
        when(processor.encrypt("hello", "default")).thenReturn("cipher");

        Object out = advice().beforeBodyWrite("hello".getBytes(StandardCharsets.UTF_8), parameter,
                MediaType.APPLICATION_JSON, null, serverRequest, serverResponse);

        assertThat(out).isEqualTo("cipher".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void beforeBodyWrite_object_encryptsToString() {
        when(parameter.getMethodAnnotation(Encrypt.class)).thenReturn(encrypt);
        when(processor.encrypt("Dto[v=x]", "default")).thenReturn("cipher");

        Object out = advice().beforeBodyWrite(new Dto("x"), parameter, MediaType.APPLICATION_JSON,
                null, serverRequest, serverResponse);

        assertThat(out).isEqualTo("cipher");
    }

    @Test
    void beforeBodyWrite_nullBody_returnsNull() {
        when(parameter.getMethodAnnotation(Encrypt.class)).thenReturn(encrypt);

        Object out = advice().beforeBodyWrite(null, parameter, MediaType.APPLICATION_JSON,
                null, serverRequest, serverResponse);

        assertThat(out).isNull();
    }

    @Test
    void beforeBodyWrite_withoutAlias_returnsBody() {
        when(parameter.getMethodAnnotation(Encrypt.class)).thenReturn(null);
        Object body = "plain";

        Object out = advice().beforeBodyWrite(body, parameter, MediaType.APPLICATION_JSON,
                null, serverRequest, serverResponse);

        assertThat(out).isSameAs(body);
    }

    @Test
    void handleEmptyBody_returnsBody() {
        Object body = new Object();

        assertThat(advice().handleEmptyBody(body, inputMessage, parameter, String.class, null)).isSameAs(body);
    }

    record Dto(String v) {
    }

    @Encrypt
    static class EncryptedController {
    }
}
