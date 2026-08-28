package cn.jowen.framework.extras.message.template;

import org.jspecify.annotations.NullMarked;

import java.util.Map;

/**
 * 模板渲染引擎抽象，将变量填充进模板得到最终文本。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface TemplateEngine {

    /**
     * 渲染模板内容。
     *
     * @param template 模板
     * @param variables 渲染变量
     * @return 渲染后的标题与内容
     */
    Rendered render(Template template, Map<String, Object> variables);

    /**
     * 渲染结果。
     *
     * @param title 渲染后的标题
     * @param content 渲染后的内容
     */
    record Rendered(String title, String content) {
    }
}
