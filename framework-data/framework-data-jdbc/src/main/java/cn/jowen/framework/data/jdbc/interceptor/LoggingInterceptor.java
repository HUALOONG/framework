package cn.jowen.framework.data.jdbc.interceptor;

import cn.jowen.framework.core.desensitize.Desensitizer;
import cn.jowen.framework.data.jdbc.config.JdbcProperties;
import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL 日志拦截器：在 SQL 执行前后打印 SQL 文本与参数（敏感参数自动脱敏）。
 *
 * <p>敏感识别：对参数值中为字符串且「看起来像」手机号/身份证/邮箱/银行卡的内容，
 * 调用 {@link Desensitizer#getInstance()} 默认规则脱敏后再打印。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class LoggingInterceptor implements SqlInterceptor {

    private final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);

    private final JdbcProperties properties;

    public LoggingInterceptor(JdbcProperties properties) {
        this.properties = properties;
    }

    @Override
    public Object intercept(SqlContext ctx, InterceptorChain chain) {
        if (properties.isSqlLogEnabled()) {
            logger.info("SQL => {} | params => {}", ctx.getSql(), maskParams(ctx.getParams()));
        }
        return ctx.proceed();
    }

    private List<Object> maskParams(List<Object> params) {
        List<Object> masked = new ArrayList<>(params.size());
        Desensitizer desensitizer = Desensitizer.getInstance();
        for (Object param : params) {
            if (param instanceof String str) {
                masked.add(desensitizer.mask(str));
            } else {
                masked.add(param);
            }
        }
        return masked;
    }
}
