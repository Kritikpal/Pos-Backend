package com.kritik.POS.user.repository;

import com.kritik.POS.user.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenId(String tokenId);

    Optional<RefreshToken> findByTokenIdAndRevokedFalse(String tokenId);

    List<RefreshToken> findAllByUserIdAndRevokedFalse(Long userId);
}
