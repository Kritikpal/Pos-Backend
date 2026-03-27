package com.kritik.POS.security.entryPoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kritik.POS.common.enums.ResponseCode;
import com.kritik.POS.common.model.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JWTEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException exception) throws IOException {

        ObjectMapper objectMapper = new ObjectMapper();

        ApiResponse<String> apiResponse = new ApiResponse<>(
                null,
                ResponseCode.UNAUTHORIZED,
                "Authentication is required to access this resource"
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
        response.getWriter().flush();
    }
}
