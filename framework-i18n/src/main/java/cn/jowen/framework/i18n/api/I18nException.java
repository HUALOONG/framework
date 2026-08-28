package cn.jowen.framework.i18n.api;

import cn.jowen.framework.core.exception.ErrorCode;
import cn.jowen.framework.core.exception.FrameworkException;
import org.jspecify.annotations.NullMarked;

/**
 * 国际化异常基类，所有 i18n 相关异常的父类。
 *
 * <p>错误码以 {@link #i18n()} 开头的域划分：消息未找到、资源加载失败、格式化失败三类，
 * 统一承载于 {@link I18nErrorCode}。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class I18nException extends FrameworkException {

    public I18nException(String message) {
        super(message);
    }

    public I18nException(String message, Throwable cause) {
        super(message, cause);
    }

    public I18nException(ErrorCode errorCode) {
        super(errorCode);
    }

    public I18nException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public I18nException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public I18nException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message);
        initCause(cause);
    }

    /**
     * 国际化错误码域。
     */
    public enum I18nErrorCode implements ErrorCode {

        /**
         * 消息未找到。
         */
        MESSAGE_NOT_FOUND("I18N_001", "消息未找到"),

        /**
         * 资源加载失败。
         */
        RESOURCE_LOAD_FAILED("I18N_002", "资源加载失败"),

        /**
         * 格式化失败。
         */
        FORMAT_FAILED("I18N_003", "消息格式化失败");

        private final String code;
        private final String message;

        I18nErrorCode(String code, String message) {
            this.code = code;
            this.message = message;
        }

        @Override
        public String code() {
            return code;
        }

        @Override
        public String message() {
            return message;
        }
    }
}
