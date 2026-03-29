package com.kritik.POS.user.service;

public interface MailService {
    void sendNewUserEmail(String to, String rawPassword);

    void sendPasswordResetEmail(String to, String resetToken);
}
