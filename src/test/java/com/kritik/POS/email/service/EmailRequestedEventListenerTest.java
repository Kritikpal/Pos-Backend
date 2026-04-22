package com.kritik.POS.email.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.kritik.POS.email.api.models.AccountCreatedEmailRequested;
import com.kritik.POS.email.api.models.PasswordResetEmailRequested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailRequestedEventListenerTest {

    @Mock
    private EmailDispatchService emailDispatchService;

    @InjectMocks
    private EmailRequestedEventListener listener;

    @Test
    void handleAccountCreatedDelegatesToDispatchService() {
        listener.handle(new AccountCreatedEmailRequested("user@example.com", "Secret123"));

        verify(emailDispatchService).sendAccountCreatedEmail("user@example.com", "Secret123");
    }

    @Test
    void handlePasswordResetDelegatesToDispatchService() {
        listener.handle(new PasswordResetEmailRequested("user@example.com", "reset-token"));

        verify(emailDispatchService).sendPasswordResetEmail("user@example.com", "reset-token");
    }

    @Test
    void handleAccountCreatedSwallowsDispatchFailures() {
        doThrow(new RuntimeException("SMTP unavailable"))
                .when(emailDispatchService)
                .sendAccountCreatedEmail("user@example.com", "Secret123");

        assertThatCode(() -> listener.handle(new AccountCreatedEmailRequested("user@example.com", "Secret123")))
                .doesNotThrowAnyException();
    }
}
