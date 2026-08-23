package cn.jowen.framework.data.mybatis.adapter;

import cn.jowen.framework.data.core.exception.DataAccessException;
import cn.jowen.framework.data.core.exception.ExceptionTranslator;
import cn.jowen.framework.data.mybatis.exception.FlexExceptionConverter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Callable;

@NullMarked
public final class FlexExceptionTranslator implements ExceptionTranslator {

    private static final ExceptionTranslator INSTANCE = new FlexExceptionTranslator();

    private FlexExceptionTranslator() {}

    public static ExceptionTranslator getInstance() { return INSTANCE; }

    @Override
    @Nullable
    public DataAccessException translate(String action, Callable<Void> task) {
        if (action == null) throw new IllegalArgumentException("action must not be null");
        if (task == null) throw new IllegalArgumentException("task must not be null");
        try {
            task.call();
            return null;
        } catch (Exception e) {
            return translateInternal(action, e);
        }
    }

    public static DataAccessException translate(String action, @Nullable Class<?> entityClass, Throwable ex) {
        if (action == null) throw new IllegalArgumentException("action must not be null");
        if (ex == null) throw new IllegalArgumentException("ex must not be null");
        String prefix = action + (entityClass != null ? "(" + entityClass.getSimpleName() + ")" : "");
        return translateInternal(prefix, ex);
    }

    private static DataAccessException translateInternal(String action, Throwable ex) {
        String prefix = "[MyBatis " + action + "] ";
        DataAccessException translated = FlexExceptionConverter.INSTANCE.translate(ex);
        translated.addSuppressed(ex);
        return translated;
    }
}
