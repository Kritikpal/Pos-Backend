package com.kritik.POS.email.service;

import com.kritik.POS.email.api.models.AccountCreatedEmailRequested;
import com.kritik.POS.email.api.models.PasswordResetEmailRequested;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EmailRequestedEventListener {

    private static final Logger log = LoggerFactory.getLogger(EmailRequestedEventListener.class);

    private final EmailDispatchService emailDispatchService;

    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(AccountCreatedEmailRequested event) {
        try {
            emailDispatchService.sendAccountCreatedEmail(event.to(), event.rawPassword());
        } catch (Exception exception) {
            log.error("Failed to send account created email to {}", event.to(), exception);
        }
    }

    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PasswordResetEmailRequested event) {
        try {
            emailDispatchService.sendPasswordResetEmail(event.to(), event.resetToken());
        } catch (Exception exception) {
            log.error("Failed to send password reset email to {}", event.to(), exception);
        }
    }
}
