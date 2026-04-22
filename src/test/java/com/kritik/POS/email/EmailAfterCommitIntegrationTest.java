package com.kritik.POS.email;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kritik.POS.PosApplication;
import com.kritik.POS.email.api.models.AccountCreatedEmailRequested;
import com.kritik.POS.email.service.EmailDispatchService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(
        classes = PosApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:email-module;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.mail.username=no-reply@example.com",
                "app.email.login-url=http://localhost:3000/login",
                "app.email.password-reset-url=http://localhost:3000/reset-password"
        }
)
@ActiveProfiles("test")
class EmailAfterCommitIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @SpyBean
    private EmailDispatchService emailDispatchService;

    @MockBean
    private JavaMailSender mailSender;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    void accountCreatedEmailIsDispatchedOnlyAfterTransactionCommit() {
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new AccountCreatedEmailRequested("user@example.com", "Secret123"));

            verify(emailDispatchService, never()).sendAccountCreatedEmail(anyString(), anyString());
        });

        verify(emailDispatchService, timeout(1000)).sendAccountCreatedEmail("user@example.com", "Secret123");
    }
}
