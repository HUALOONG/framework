package cn.jowen.framework.plugin.extension;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 扩展扫描包路径标注。
 *
 * <p>示例：
 * <pre>{@code
 * @ExtensionScan(basePackages = "com.example.plugin.extensions")
 * }</pre>
 *
 * @author 王飞
 */
@NullMarked
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ExtensionScan {

    /**
     * 扫描的基础包路径列表。
     */
    String[] basePackages();
}
