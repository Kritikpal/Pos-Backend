package com.kritik.POS.email.service;

public interface EmailDispatchService {

    void sendAccountCreatedEmail(String to, String rawPassword);

    void sendPasswordResetEmail(String to, String resetToken);
}
