package cn.jowen.framework.data.core.exception;

import cn.jowen.framework.core.spi.SPI;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Callable;

/**
 * 数据访问异常翻译器 SPI 接口，用于将底层数据访问异常转换为框架统一的异常类型。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@SPI
public interface ExceptionTranslator {

    /**
     * 将指定操作的底层异常翻译为框架数据访问异常。
     *
     * <p>若 task 执行过程中未抛出异常，则返回 {@code null}。
     *
     * @param action 描述当前操作的字符串，用于异常信息构建，不可为 {@code null}
     * @param task   要执行的任务，抛出受检异常时将尝试翻译，不可为 {@code null}
     * @return 翻译后的数据访问异常；若无需翻译或未发生异常则返回 {@code null}
     */
    @Nullable
    DataAccessException translate(String action, Callable<Void> task);
}
