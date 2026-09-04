package cn.jowen.framework.extras.web.excel;

import org.jspecify.annotations.NullMarked;

/**
 * Excel 处理异常：导入导出过程中的行数超限、格式错误等统一以本异常抛出。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class ExcelException extends RuntimeException {

    /**
     * 序列化版本号。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 构造实例。
     *
     * @param message 异常信息
     */
    public ExcelException(String message) {
        super(message);
    }

    /**
     * 构造实例。
     *
     * @param message 异常信息
     * @param cause   根因
     */
    public ExcelException(String message, Throwable cause) {
        super(message, cause);
    }
}
