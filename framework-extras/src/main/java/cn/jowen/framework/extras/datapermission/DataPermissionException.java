package cn.jowen.framework.extras.datapermission;

import cn.jowen.framework.core.exception.BusinessException;
import org.jspecify.annotations.NullMarked;

/**
 * 数据权限业务异常（如非法列名、规则计算失败等可预期错误）。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public class DataPermissionException extends BusinessException {

    public DataPermissionException(String message) {
        super(message);
    }

    public DataPermissionException(String message, Throwable cause) {
        super(message, cause);
    }
}
