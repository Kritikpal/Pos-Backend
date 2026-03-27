package com.kritik.POS.user.service.impl;

import com.kritik.POS.exception.errors.BadRequestException;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.restaurant.entity.Restaurant;
import com.kritik.POS.restaurant.repository.RestaurantRepository;
import com.kritik.POS.security.service.TenantAccessService;
import com.kritik.POS.security.util.JwtUtil;
import com.kritik.POS.user.entity.*;
import com.kritik.POS.user.model.request.UserCreateRequest;
import com.kritik.POS.user.model.request.UserUpdateRequest;
import com.kritik.POS.user.model.response.UserProjection;
import com.kritik.POS.user.model.response.UserResponse;
import com.kritik.POS.user.repository.*;
import com.kritik.POS.user.service.MailService;
import com.kritik.POS.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RestaurantRepository restaurantRepository;
    private final TenantAccessService tenantAccessService;
    private final MailService mailService;
    private final PasswordResetRequestRepository passwordResetRequestRepository;
    private final JwtUtil jwtUtil;

    private Role resolveRole(String roleName) {
        return roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new AppException(roleName + " role not found", HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private UserResponse toUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setUserId(user.getUserId());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setChainId(user.getChainId());
        response.setRestaurantId(user.getRestaurantId());
        response.setRoles(user.getRoles().stream().map(Role::getRoleName).collect(Collectors.toSet()));
        return response;
    }

    private String generatePassword() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private void assertUniqueEmail(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BadRequestException("User already exists");
        }
    }

    @Override
    public void createRestaurantAdmin(Long chainId, Long restaurantId, String email, String phone, String password) {
        assertUniqueEmail(email);

        Role role = resolveRole("RESTAURANT_ADMIN");

        User user = new User();
        user.setEmail(email);
        user.setPhoneNumber(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setChainId(chainId);
        user.setRestaurantId(restaurantId);
        user.setRoles(Set.of(role));

        userRepository.save(user);
        mailService.sendNewUserEmail(email, password);
    }

    @Override
    public void createChainAdmin(Long chainId, String email, String phone, String password) {
        assertUniqueEmail(email);

        Role role = resolveRole("CHAIN_ADMIN");

        User user = new User();
        user.setEmail(email);
        user.setPhoneNumber(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setChainId(chainId);
        user.setRoles(Set.of(role));

        userRepository.save(user);
        mailService.sendNewUserEmail(email, password);
    }

    @Override
    public void createSuperAdmin(String email, String phone, String password) {
        assertUniqueEmail(email);

        Role role = resolveRole("SUPER_ADMIN");

        User user = new User();
        user.setEmail(email);
        user.setPhoneNumber(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(Set.of(role));

        userRepository.save(user);
        mailService.sendNewUserEmail(email, password);
    }


    @Override
    public void validateUserNotExists(String email) {
        assertUniqueEmail(email);
    }

    @Override
    public UserResponse createUser(UserCreateRequest request) {
        assertUniqueEmail(request.getEmail());

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setChainId(request.getChainId());
        user.setRestaurantId(request.getRestaurantId());

        Role role = resolveRole(request.getRole());
        user.setRoles(Set.of(role));

        User saved = userRepository.save(user);
        mailService.sendNewUserEmail(saved.getEmail(), request.getPassword());
        return toUserResponse(saved);
    }

    @Override
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        // scope check
        if (tenantAccessService.currentUser().isSuperAdmin()) {
            // no extra check
        } else if (tenantAccessService.currentUser().isChainAdmin()) {
            Long currentChainId = tenantAccessService.currentUser().getChainId();
            if (user.getChainId() == null || !user.getChainId().equals(currentChainId)) {
                throw new BadRequestException("Access denied");
            }
        } else if (tenantAccessService.currentUser().isRestaurantAdmin()) {
            Long currentRestaurantId = tenantAccessService.currentUser().getRestaurantId();
            if (user.getRestaurantId() == null || !user.getRestaurantId().equals(currentRestaurantId)) {
                throw new BadRequestException("Access denied");
            }
            boolean notStaff = user.getRoles().stream().noneMatch(r -> r.getRoleName().equals("STAFF"));
            if (notStaff) {
                throw new BadRequestException("Restaurant manager can only manage staff");
            }
        } else {
            throw new BadRequestException("Access denied");
        }

        if (request.getEmail() != null && !request.getEmail().isBlank() && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new BadRequestException("Email already in use");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getRole() != null && !request.getRole().isBlank()) {
            Role newRole = resolveRole(request.getRole());
            user.setRoles(Set.of(newRole));
        }
        if (request.getChainId() != null) {
            user.setChainId(request.getChainId());
        }
        if (request.getRestaurantId() != null) {
            user.setRestaurantId(request.getRestaurantId());
        }

        User updated = userRepository.save(user);
        return toUserResponse(updated);
    }

    @Override
    public UserResponse getUser(Long userId) {
        return userRepository.findById(userId)
                .map(this::toUserResponse)
                .orElseThrow(() -> new BadRequestException("User not found"));
    }

    @Override
    public Page<UserProjection> getUsers(Long chainId, Long restaurantId, String search, Pageable pageable) {
        if (tenantAccessService.currentUser().isSuperAdmin()) {
            return userRepository.findUsers(chainId, restaurantId, search, pageable);
        }

        if (tenantAccessService.currentUser().isChainAdmin()) {
            Long fixedChainId = tenantAccessService.currentUser().getChainId();
            if (restaurantId != null && !restaurantRepository.existsByRestaurantIdAndChainIdAndIsDeletedFalse(restaurantId, fixedChainId)) {
                throw new BadRequestException("Restaurant access denied");
            }
            return userRepository.findUsers(fixedChainId, restaurantId, search, pageable);
        }

        if (tenantAccessService.currentUser().isRestaurantAdmin()) {
            Long fixedRestaurantId = tenantAccessService.currentUser().getRestaurantId();
            return userRepository.findUsers(null, fixedRestaurantId, search, pageable);
        }

        throw new BadRequestException("Access denied");
    }

    @Override
    public Page<UserProjection> getChainAdmins(Long chainId, String search, Pageable pageable) {
        if (tenantAccessService.currentUser().isSuperAdmin()) {
            return userRepository.findUsersByRole("CHAIN_ADMIN", chainId, null, search, pageable);
        }
        if (tenantAccessService.currentUser().isChainAdmin()) {
            Long fixedChainId = tenantAccessService.currentUser().getChainId();
            return userRepository.findUsersByRole("CHAIN_ADMIN", fixedChainId, null, search, pageable);
        }
        throw new BadRequestException("Access denied");
    }

    @Override
    public Page<UserProjection> getRestaurantAdmins(Long chainId, Long restaurantId, String search, Pageable pageable) {
        if (tenantAccessService.currentUser().isSuperAdmin()) {
            return userRepository.findUsersByRole("RESTAURANT_ADMIN", chainId, restaurantId, search, pageable);
        }
        if (tenantAccessService.currentUser().isChainAdmin()) {
            Long fixedChainId = tenantAccessService.currentUser().getChainId();
            if (chainId != null && !chainId.equals(fixedChainId)) {
                throw new BadRequestException("Chain access denied");
            }
            if (restaurantId != null && !restaurantRepository.existsByRestaurantIdAndChainIdAndIsDeletedFalse(restaurantId, fixedChainId)) {
                throw new BadRequestException("Restaurant access denied");
            }
            return userRepository.findUsersByRole("RESTAURANT_ADMIN", fixedChainId, restaurantId, search, pageable);
        }
        if (tenantAccessService.currentUser().isRestaurantAdmin()) {
            Long fixedRestaurantId = tenantAccessService.currentUser().getRestaurantId();
            return userRepository.findUsersByRole("RESTAURANT_ADMIN", null, fixedRestaurantId, search, pageable);
        }
        throw new BadRequestException("Access denied");
    }

    @Override
    public Page<UserProjection> getStaffs(Long chainId, Long restaurantId, String search, Pageable pageable) {
        if (tenantAccessService.currentUser().isSuperAdmin()) {
            return userRepository.findUsersByRole("STAFF", chainId, restaurantId, search, pageable);
        }
        if (tenantAccessService.currentUser().isChainAdmin()) {
            Long fixedChainId = tenantAccessService.currentUser().getChainId();
            if (restaurantId != null && !restaurantRepository.existsByRestaurantIdAndChainIdAndIsDeletedFalse(restaurantId, fixedChainId)) {
                throw new BadRequestException("Restaurant access denied");
            }
            return userRepository.findUsersByRole("STAFF", fixedChainId, restaurantId, search, pageable);
        }
        if (tenantAccessService.currentUser().isRestaurantAdmin()) {
            Long fixedRestaurantId = tenantAccessService.currentUser().getRestaurantId();
            return userRepository.findUsersByRole("STAFF", null, fixedRestaurantId, search, pageable);
        }
        throw new BadRequestException("Access denied");
    }

    @Override
    public boolean resetPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        boolean allowed = false;
        if (tenantAccessService.currentUser().isSuperAdmin()) {
            allowed = true;
        } else if (tenantAccessService.currentUser().isChainAdmin()) {
            if (user.getChainId() != null && user.getChainId().equals(tenantAccessService.currentUser().getChainId())) {
                allowed = true;
            }
        } else if (tenantAccessService.currentUser().isRestaurantAdmin()) {
            if (user.getRestaurantId() != null && user.getRestaurantId().equals(tenantAccessService.currentUser().getRestaurantId())) {
                allowed = true;
            }
        }

        if (!allowed) {
            throw new BadRequestException("Access denied");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }

    @Override
    public void createStaff(Long restaurantId, String email, String phone, String password) {
        assertUniqueEmail(email);

        Role role = resolveRole("STAFF");

        Restaurant restaurant = restaurantRepository.findByRestaurantIdAndIsDeletedFalse(restaurantId)
                .orElseThrow(() -> new BadRequestException("Restaurant not found"));

        User user = new User();
        user.setEmail(email);
        user.setPhoneNumber(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setRestaurantId(restaurantId);
        user.setChainId(restaurant.getChainId());
        user.setRoles(Set.of(role));

        userRepository.save(user);
        mailService.sendNewUserEmail(email, password);
    }

    @Override
    public boolean sendPasswordResetEmail(String email) throws AppException {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new BadRequestException("User with this email not found"));

        // Invalidate any pending reset requests for this user
        passwordResetRequestRepository.findByUserIdAndStatus(user.getUserId(), PasswordResetStatus.PENDING)
                .ifPresent(existingRequest -> {
                    existingRequest.setStatus(PasswordResetStatus.EXPIRED);
                    passwordResetRequestRepository.save(existingRequest);
                });

        // Generate tokenId (jti) and JWT token for password reset (24 hour expiry)
        String tokenId = UUID.randomUUID().toString();
        Map<String, Object> claims = Map.of(
                "email", user.getEmail(),
                "userId", user.getUserId(),
                "type", "PASSWORD_RESET",
                "jti", tokenId
        );
        String resetToken = jwtUtil.generateToken(user.getEmail(), claims, 86400); // 24 hours

        // Create password reset request record (store UUID only, not JWT)
        PasswordResetRequest resetRequest = PasswordResetRequest.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .tokenId(tokenId)
                .status(PasswordResetStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .expiryDate(LocalDateTime.now().plusDays(1))
                .build();

        passwordResetRequestRepository.save(resetRequest);

        // Send password reset email
        mailService.sendPasswordResetEmail(user.getEmail(), resetToken);

        return true;
    }

    @Override
    public boolean changePasswordFromResetToken(String token, String newPassword) throws AppException {
        // Validate token format and claims
        Map<String, Object> claims;
        try {
            claims = jwtUtil.extractAllClaims(token);
        } catch (Exception e) {
            throw new BadRequestException("Invalid or expired reset token");
        }

        String email = (String) claims.get("email");
        String tokenId = (String) claims.get("jti");
        if (email == null || tokenId == null) {
            throw new BadRequestException("Invalid reset token");
        }

        // Find the password reset request by tokenId and pending status
        PasswordResetRequest resetRequest = passwordResetRequestRepository.findByTokenIdAndStatus(tokenId, PasswordResetStatus.PENDING)
                .orElseThrow(() -> new BadRequestException("Invalid or already used reset token"));

        // Check if token is expired
        if (resetRequest.isExpired()) {
            resetRequest.setStatus(PasswordResetStatus.EXPIRED);
            passwordResetRequestRepository.save(resetRequest);
            throw new BadRequestException("Reset token has expired");
        }

        // Find user and update password
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        // Reuse the existing change password method logic
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark the reset request as verified/used
        resetRequest.setStatus(PasswordResetStatus.VERIFIED);
        resetRequest.setVerifiedAt(LocalDateTime.now());
        resetRequest.setUsedAt(LocalDateTime.now());
        passwordResetRequestRepository.save(resetRequest);

        return true;
    }

}

