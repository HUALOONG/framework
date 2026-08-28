package cn.jowen.framework.extras.message.template;

import org.jspecify.annotations.NullMarked;

/**
 * 消息模板，承载可被渲染的标题与内容模板。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class Template {

    private final String code;
    private final String titleTemplate;
    private final String contentTemplate;

    public Template(String code, String titleTemplate, String contentTemplate) {
        this.code = code;
        this.titleTemplate = titleTemplate;
        this.contentTemplate = contentTemplate;
    }

    public String getCode() {
        return code;
    }

    public String getTitleTemplate() {
        return titleTemplate;
    }

    public String getContentTemplate() {
        return contentTemplate;
    }
}
