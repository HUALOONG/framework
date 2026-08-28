package cn.jowen.framework.extras.web.crypto;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * {@link Encrypt} 注解的加解密织入。
 *
 * <p>对标注了 {@link Encrypt} 的控制器：
 * <ul>
 *   <li>入参（{@code @RequestBody}）先经 {@link CryptoProcessor#decrypt} 解密；</li>
 *   <li>出参（{@code @ResponseBody}）返回前经 {@link CryptoProcessor#encrypt} 加密。</li>
 * </ul>
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@ControllerAdvice
public final class CryptoAdvice implements RequestBodyAdvice, ResponseBodyAdvice<Object> {

    /** processor 不可变字段。 */
    private final CryptoProcessor processor;

    /**
     * 构造实例。
     * @param processor 参数 processor
     */
    public CryptoAdvice(CryptoProcessor processor) {
        this.processor = processor;
    }

    /**
     * 执行supports操作。
     * @param methodParameter 参数 methodParameter
     * @param targetType 参数 targetType
     * @return 结果
     */
    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType,
                           Class<? extends HttpMessageConverter<?>> converterType) {
        return resolveAlias(methodParameter) != null;
    }

    /**
     * 执行before body read操作。
     * @param inputMessage 参数 inputMessage
     * @param parameter 参数 parameter
     * @param targetType 参数 targetType
     * @return 结果
     * @throws IOException IOException 异常
     */
    @Override
    public @Nullable HttpInputMessage beforeBodyRead(@Nullable HttpInputMessage inputMessage,
                                                     MethodParameter parameter, Type targetType,
                                                     Class<? extends HttpMessageConverter<?>> converterType)
            throws IOException {
        String alias = resolveAlias(parameter);
        if (inputMessage == null || alias == null) {
            return inputMessage;
        }
        byte[] raw = inputMessage.getBody().readAllBytes();
        String plain = processor.decrypt(new String(raw, StandardCharsets.UTF_8), alias);
        byte[] decoded = plain.getBytes(StandardCharsets.UTF_8);
        final InputStream decrypted = new ByteArrayInputStream(decoded);
        return new HttpInputMessage() {
            @Override
            public InputStream getBody() {
                return decrypted;
            }

            @Override
            public org.springframework.http.HttpHeaders getHeaders() {
                return inputMessage.getHeaders();
            }
        };
    }

    /**
     * 执行after body read操作。
     * @param body 参数 body
     * @param inputMessage 参数 inputMessage
     * @param parameter 参数 parameter
     * @param targetType 参数 targetType
     * @return 结果
     */
    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }

    /**
     * 执行supports操作。
     * @param returnType 参数 returnType
     * @return 结果
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return resolveAlias(returnType) != null;
    }

    /**
     * 执行before body write操作。
     * @param body 参数 body
     * @param returnType 参数 returnType
     * @param selectedContentType 参数 selectedContentType
     * @param request 参数 request
     * @param response 参数 response
     * @return 结果
     */
    @Override
    public @Nullable Object beforeBodyWrite(@Nullable Object body, MethodParameter returnType,
                                            MediaType selectedContentType,
                                            Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                            ServerHttpRequest request, ServerHttpResponse response) {
        String alias = resolveAlias(returnType);
        if (alias == null || body == null) {
            return body;
        }
        if (body instanceof String s) {
            return processor.encrypt(s, alias);
        }
        if (body instanceof byte[] b) {
            return processor.encrypt(new String(b, StandardCharsets.UTF_8), alias).getBytes(StandardCharsets.UTF_8);
        }
        return processor.encrypt(body.toString(), alias);
    }

    private @Nullable String resolveAlias(MethodParameter parameter) {
        Encrypt ann = parameter.getMethodAnnotation(Encrypt.class);
        if (ann == null && parameter.getContainingClass().isAnnotationPresent(Encrypt.class)) {
            ann = parameter.getContainingClass().getAnnotation(Encrypt.class);
        }
        return ann == null ? null : ann.keyAlias();
    }

    /**
     * 执行handle empty body操作。
     * @param body 参数 body
     * @param inputMessage 参数 inputMessage
     * @param parameter 参数 parameter
     * @param targetType 参数 targetType
     * @return 结果
     */
    @Override
    public Object handleEmptyBody(@Nullable Object body, HttpInputMessage inputMessage,
                                  MethodParameter parameter, Type targetType,
                                  Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }
}
