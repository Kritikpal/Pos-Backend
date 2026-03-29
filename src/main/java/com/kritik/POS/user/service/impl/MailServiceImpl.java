package com.kritik.POS.user.service.impl;

import com.kritik.POS.user.service.MailService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Value("${app.login-url:http://localhost:8080/login}")
    private String loginUrl;

    @Value("${app.password-reset-url:http://localhost:8080/reset-password}")
    private String passwordResetUrl;

    @Value("${spring.mail.username:no-reply@example.com}")
    private String from;

    @PostConstruct
    public void init() {
        // no-op for now, useful for logging diagnostics
    }

    @Override
    public void sendNewUserEmail(String to, String rawPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Your POS account was created");
        message.setText("Hello,\n\n" +
                "Your account has been created with the following details:\n" +
                "Email: " + to + "\n" +
                "Password: " + rawPassword + "\n\n" +
                "Go to: " + loginUrl + "\n\n" +
                "Please change the password after sign-in.\n\n" +
                "Thanks.\n");

        mailSender.send(message);
    }

    @Override
    public void sendPasswordResetEmail(String to, String resetToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Password Reset Request");
        message.setText("Hello,\n\n" +
                "We received a request to reset your POS account password.\n\n" +
                "Click the link below to reset your password:\n" +
                passwordResetUrl + "?token=" + resetToken + "\n\n" +
                "This link will expire in 24 hours.\n\n" +
                "If you did not request this, please ignore this email.\n\n" +
                "Thanks.\n");

        mailSender.send(message);
    }
}
