package cn.jowen.framework.extras.message.core;

import org.jspecify.annotations.NullMarked;

/**
 * 消息模板描述：类型 + 标题模板 + 内容模板。
 *
 * @param type            消息类型
 * @param titleTemplate   标题模板（支持 {@code ${key}} 占位符）
 * @param contentTemplate 内容模板（支持 {@code ${key}} 占位符）
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public record MessageTemplate(MessageType type, String titleTemplate, String contentTemplate) {
}
