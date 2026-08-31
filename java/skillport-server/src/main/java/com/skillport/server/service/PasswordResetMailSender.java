package com.skillport.server.service;

import com.skillport.server.config.SkillPortProperties;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Component
public class PasswordResetMailSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetMailSender.class);
    private final SkillPortProperties.Mail properties;

    public PasswordResetMailSender(SkillPortProperties properties) {
        this.properties = properties.mail();
    }

    public boolean configured() {
        return properties != null && properties.configured();
    }

    public void sendCode(String email, String displayName, String code) {
        if (!configured()) throw new IllegalStateException("SkillPort mail delivery is not configured");
        try {
            JavaMailSenderImpl sender = sender();
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.from());
            helper.setTo(email);
            helper.setSubject("SkillPort 密码重置验证码");
            helper.setText("你好，" + displayName + "：\n\n"
                    + "你正在重置 SkillPort 登录密码，验证码为：" + code + "\n\n"
                    + "验证码 10 分钟内有效，请勿转发给他人。若不是你本人操作，请忽略此邮件。\n\n"
                    + "SkillPort", false);
            sender.send(message);
        } catch (Exception exception) {
            LOGGER.warn("Password reset email delivery failed for domain={}", emailDomain(email));
            throw new IllegalStateException("Password reset email delivery failed", exception);
        }
    }

    private JavaMailSenderImpl sender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(properties.host());
        sender.setPort(properties.port() > 0 ? properties.port() : 465);
        sender.setUsername(properties.username());
        sender.setPassword(properties.password());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        Properties javaMail = sender.getJavaMailProperties();
        javaMail.put("mail.smtp.auth", "true");
        javaMail.put("mail.smtp.ssl.enable", Boolean.toString(properties.ssl()));
        javaMail.put("mail.smtp.starttls.enable", Boolean.toString(properties.starttls()));
        javaMail.put("mail.smtp.connectiontimeout", "5000");
        javaMail.put("mail.smtp.timeout", "10000");
        javaMail.put("mail.smtp.writetimeout", "10000");
        return sender;
    }

    private static String emailDomain(String email) {
        int separator = email.lastIndexOf('@');
        return separator >= 0 ? email.substring(separator + 1) : "unknown";
    }
}
