package com.kritik.POS.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kritik.POS.email.api.models.AccountCreatedEmailRequested;
import com.kritik.POS.email.api.models.PasswordResetEmailRequested;
import com.kritik.POS.restaurant.repository.RestaurantRepository;
import com.kritik.POS.security.service.TenantAccessService;
import com.kritik.POS.security.util.JwtUtil;
import com.kritik.POS.user.entity.PasswordResetRequest;
import com.kritik.POS.user.entity.PasswordResetStatus;
import com.kritik.POS.user.entity.Role;
import com.kritik.POS.user.entity.User;
import com.kritik.POS.user.model.request.UserCreateRequest;
import com.kritik.POS.user.model.response.UserResponse;
import com.kritik.POS.user.repository.PasswordResetRequestRepository;
import com.kritik.POS.user.repository.RoleRepository;
import com.kritik.POS.user.repository.UserRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private TenantAccessService tenantAccessService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PasswordResetRequestRepository passwordResetRequestRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void createUserPublishesAccountCreatedEmailRequestedEvent() {
        UserCreateRequest request = new UserCreateRequest(
                "user@example.com",
                "Secret123",
                "9876543210",
                "STAFF",
                1L,
                2L
        );
        Role role = new Role();
        role.setRoleName("STAFF");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByRoleName("STAFF")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("Secret123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(10L);
            return user;
        });

        UserResponse response = userService.createUser(request);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(new AccountCreatedEmailRequested("user@example.com", "Secret123"));
        assertThat(response.getUserId()).isEqualTo(10L);
        assertThat(response.getEmail()).isEqualTo("user@example.com");
        assertThat(response.getRoles()).isEqualTo(Set.of("STAFF"));
    }

    @Test
    void sendPasswordResetEmailPublishesPasswordResetRequestedEventAfterPersistingResetRequest() {
        User user = new User();
        user.setUserId(21L);
        user.setEmail("user@example.com");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordResetRequestRepository.findByUserIdAndStatus(21L, PasswordResetStatus.PENDING)).thenReturn(Optional.empty());
        when(jwtUtil.generateToken(eq("user@example.com"), anyMap(), eq(86400))).thenReturn("reset-jwt-token");
        when(passwordResetRequestRepository.save(any(PasswordResetRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean sent = userService.sendPasswordResetEmail(" user@example.com ");

        ArgumentCaptor<PasswordResetRequest> resetRequestCaptor = ArgumentCaptor.forClass(PasswordResetRequest.class);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);

        verify(passwordResetRequestRepository).save(resetRequestCaptor.capture());
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        assertThat(sent).isTrue();
        assertThat(resetRequestCaptor.getValue().getUserId()).isEqualTo(21L);
        assertThat(resetRequestCaptor.getValue().getEmail()).isEqualTo("user@example.com");
        assertThat(resetRequestCaptor.getValue().getStatus()).isEqualTo(PasswordResetStatus.PENDING);
        assertThat(eventCaptor.getValue()).isEqualTo(new PasswordResetEmailRequested("user@example.com", "reset-jwt-token"));
    }
}
