package cn.jowen.framework.extras.notification.template;

/**
 * 通知模板引擎接口。
 *
 * <p>支持简单占位符（{@code ${name}}）与 FreeMarker 模板语法，
 * 用于邮件、短信、钉钉、企微等渠道的消息内容渲染。
 *
 * @author 王飞
 * @since 2026-08-25
 */
public interface NotificationTemplateEngine {

    /**
     * 渲染模板。
     *
     * @param template 模板字符串
     * @param params   模板参数，不可为 {@code null}
     * @return 渲染后的消息内容
     */
    String render(String template, Object params);
}
