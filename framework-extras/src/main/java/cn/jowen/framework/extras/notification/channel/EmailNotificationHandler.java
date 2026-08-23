package cn.jowen.framework.extras.notification.channel;

import cn.jowen.framework.extras.notification.NotificationChannel;
import cn.jowen.framework.extras.notification.NotificationChannelHandler;
import cn.jowen.framework.extras.notification.NotificationRequest;
import cn.jowen.framework.extras.notification.NotificationResult;
import cn.jowen.framework.extras.notification.config.EmailProperties;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * Email 通知渠道处理器（基于 Jakarta Mail / Angus Mail）。
 *
 * <p>依赖 {@code org.eclipse.angus:angus-mail}（optional）；未在 classpath 时由
 * {@code @ConditionalOnClass} 保证不注册。
 *
 * <p>缺少必要配置（host / from）时返回 {@code success=false}，**不抛异常**。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public final class EmailNotificationHandler implements NotificationChannelHandler {

    private final EmailProperties props;

    public EmailNotificationHandler(EmailProperties props) {
        this.props = java.util.Objects.requireNonNull(props, "props must not be null");
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL;
    }

    @Override
    public NotificationResult send(NotificationRequest req) {
        if (req.channel() != NotificationChannel.EMAIL) {
            return NotificationResult.failure(NotificationChannel.EMAIL, "不支持的渠道");
        }
        if (props.getHost() == null || props.getHost().isBlank()) {
            return NotificationResult.failure(NotificationChannel.EMAIL, "email.host 未配置");
        }
        String to = req.to();
        if (to == null || to.isBlank()) {
            return NotificationResult.failure(NotificationChannel.EMAIL, "接收方邮箱不能为空");
        }
        String subject = req.subject();
        String content = req.content();
        if (subject == null) {
            subject = "";
        }
        if (content == null) {
            content = "";
        }

        try {
            java.util.Properties mailProps = new java.util.Properties();
            mailProps.putAll(props.getProperties());
            mailProps.setProperty("mail.transport.protocol", "smtp");
            mailProps.setProperty("mail.smtp.host", props.getHost());
            mailProps.setProperty("mail.smtp.port", String.valueOf(props.getPort()));
            mailProps.setProperty("mail.smtp.auth", "true");
            if (props.isSsl()) {
                mailProps.setProperty("mail.smtp.ssl.enable", "true");
            } else {
                mailProps.setProperty("mail.smtp.starttls.enable", "true");
            }

            Session session = Session.getInstance(mailProps);
            MimeMessage message = new MimeMessage(session);
            String from = props.getFrom() != null ? props.getFrom() : props.getUsername();
            message.setFrom(new InternetAddress(from == null ? "" : from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject, "UTF-8");
            message.setText(content, "UTF-8");
            message.setSentDate(new java.util.Date());

            String username = props.getUsername();
            String password = props.getPassword();
            if (username != null && password != null) {
                Transport.send(message, username, password);
            } else {
                // 无凭据时尝试匿名发送（某些内部 SMTP 允许）
                Transport.send(message);
            }

            return NotificationResult.success(NotificationChannel.EMAIL, UUID.randomUUID().toString());
        } catch (MessagingException e) {
            return NotificationResult.failure(NotificationChannel.EMAIL, "邮件发送失败: " + e.getMessage());
        } catch (Exception e) {
            return NotificationResult.failure(NotificationChannel.EMAIL, "邮件发送异常: " + e.getMessage());
        }
    }
}
