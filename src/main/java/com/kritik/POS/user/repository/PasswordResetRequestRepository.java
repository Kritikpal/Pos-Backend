package com.kritik.POS.user.repository;

import com.kritik.POS.user.entity.PasswordResetRequest;
import com.kritik.POS.user.entity.PasswordResetStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, Long> {

    Optional<PasswordResetRequest> findByTokenId(String tokenId);

    Optional<PasswordResetRequest> findByTokenIdAndStatus(String tokenId, PasswordResetStatus status);

    Optional<PasswordResetRequest> findByUserIdAndStatus(Long userId, PasswordResetStatus status);
}
