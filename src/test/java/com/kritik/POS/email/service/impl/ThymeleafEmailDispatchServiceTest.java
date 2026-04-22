package com.kritik.POS.email.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kritik.POS.email.config.EmailProperties;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.IContext;
import org.thymeleaf.spring6.SpringTemplateEngine;

@ExtendWith(MockitoExtension.class)
class ThymeleafEmailDispatchServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SpringTemplateEngine templateEngine;

    private ThymeleafEmailDispatchService emailDispatchService;

    private EmailProperties emailProperties;
    private MailProperties mailProperties;

    @BeforeEach
    void setUp() {
        emailProperties = new EmailProperties();
        emailProperties.setLoginUrl("http://localhost:3000/login");
        emailProperties.setPasswordResetUrl("http://localhost:3000/reset-password");
        emailProperties.setFromName("POS Support");

        mailProperties = new MailProperties();
        mailProperties.setUsername("no-reply@example.com");

        emailDispatchService = new ThymeleafEmailDispatchService(mailSender, templateEngine, emailProperties, mailProperties);
    }

    @Test
    void sendAccountCreatedEmailUsesHtmlTemplateAndMimeMessage() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<IContext> contextCaptor = ArgumentCaptor.forClass(IContext.class);

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/account-created"), contextCaptor.capture()))
                .thenReturn("<html><body>Account created</body></html>");

        emailDispatchService.sendAccountCreatedEmail("user@example.com", "Secret123");

        verify(mailSender).send(mimeMessage);
        assertThat(mimeMessage.getSubject()).isEqualTo("Your POS account was created");
        assertThat(extractHtml(mimeMessage)).contains("Account created");
        assertThat(contextCaptor.getValue().getVariable("email")).isEqualTo("user@example.com");
        assertThat(contextCaptor.getValue().getVariable("rawPassword")).isEqualTo("Secret123");
        assertThat(contextCaptor.getValue().getVariable("loginUrl")).isEqualTo("http://localhost:3000/login");
    }

    @Test
    void sendPasswordResetEmailUsesHtmlTemplateAndResetUrl() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<IContext> contextCaptor = ArgumentCaptor.forClass(IContext.class);

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/password-reset"), contextCaptor.capture()))
                .thenReturn("<html><body>Password reset</body></html>");

        emailDispatchService.sendPasswordResetEmail("user@example.com", "reset-token");

        verify(mailSender).send(mimeMessage);
        assertThat(mimeMessage.getSubject()).isEqualTo("Password Reset Request");
        assertThat(extractHtml(mimeMessage)).contains("Password reset");
        assertThat(contextCaptor.getValue().getVariable("email")).isEqualTo("user@example.com");
        assertThat(contextCaptor.getValue().getVariable("resetToken")).isEqualTo("reset-token");
        assertThat(contextCaptor.getValue().getVariable("passwordResetUrl")).isEqualTo("http://localhost:3000/reset-password");
    }

    private String extractHtml(MimeMessage mimeMessage) throws Exception {
        Object content = mimeMessage.getContent();
        if (content instanceof String stringContent) {
            return stringContent;
        }
        if (content instanceof Multipart multipart) {
            BodyPart bodyPart = multipart.getBodyPart(0);
            return String.valueOf(bodyPart.getContent());
        }
        return String.valueOf(content);
    }
}
