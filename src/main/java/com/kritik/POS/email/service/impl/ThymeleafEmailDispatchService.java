package com.kritik.POS.email.service.impl;

import com.kritik.POS.email.config.EmailProperties;
import com.kritik.POS.email.service.EmailDispatchService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@RequiredArgsConstructor
public class ThymeleafEmailDispatchService implements EmailDispatchService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final EmailProperties emailProperties;
    private final MailProperties mailProperties;

    @Override
    public void sendAccountCreatedEmail(String to, String rawPassword) {
        Context context = new Context();
        context.setVariable("email", to);
        context.setVariable("rawPassword", rawPassword);
        context.setVariable("loginUrl", emailProperties.getLoginUrl());

        String html = templateEngine.process("email/account-created", context);
        sendHtmlEmail(to, "Your POS account was created", html);
    }

    @Override
    public void sendPasswordResetEmail(String to, String resetToken) {
        Context context = new Context();
        context.setVariable("email", to);
        context.setVariable("resetToken", resetToken);
        context.setVariable("passwordResetUrl", emailProperties.getPasswordResetUrl());

        String html = templateEngine.process("email/password-reset", context);
        sendHtmlEmail(to, "Password Reset Request", html);
    }

    private void sendHtmlEmail(String to, String subject, String html) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom(resolveFromEmail(), emailProperties.getFromName());
            helper.setText(html, true);

            mailSender.send(mimeMessage);
        } catch (MessagingException | UnsupportedEncodingException exception) {
            throw new MailPreparationException("Failed to prepare email for " + to, exception);
        }
    }

    private String resolveFromEmail() {
        String configuredUsername = mailProperties.getUsername();
        if (configuredUsername == null || configuredUsername.isBlank()) {
            return "no-reply@example.com";
        }
        return configuredUsername;
    }
}
