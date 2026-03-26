package com.kritik.POS.security.util;

import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.security.models.SecurityUser;
import com.kritik.POS.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

public class SecurityUtil {
    public static SecurityUser securityContextHolder() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AppException("User not present in the context", HttpStatus.CONFLICT);
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof SecurityUser securityUser) {
            return securityUser;
        }
        throw new AppException("User not present in the context", HttpStatus.CONFLICT);
    }


}
