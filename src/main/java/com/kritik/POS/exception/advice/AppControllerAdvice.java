package com.kritik.POS.exception.advice;

import com.kritik.POS.common.enums.ResponseCode;
import com.kritik.POS.common.model.ApiResponse;
import com.kritik.POS.exception.errors.AppAuthenticationException;
import com.kritik.POS.exception.errors.AppException;
import com.opencsv.exceptions.CsvValidationException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.IOException;
import java.util.List;

@RestControllerAdvice
public class AppControllerAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler({AppAuthenticationException.class})
    public ResponseEntity<ApiResponse<Object>> handleAppAuthenticationException(HttpServletResponse response, AppAuthenticationException appAuthenticationException) {
        ApiResponse<Object> apiResponse = new ApiResponse<>(null,
                ResponseCode.UNAUTHORIZED, appAuthenticationException.getMessage());
        return new ResponseEntity<>(apiResponse, HttpStatus.UNAUTHORIZED);

    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ObjectError> allErrors = ex.getBindingResult().getAllErrors();
        if (!allErrors.isEmpty()) {
            ApiResponse<Object> apiResponse = new ApiResponse<>(null,
                    ResponseCode.ERROR, allErrors.get(0).getDefaultMessage());
            return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
        }
        return super.handleMethodArgumentNotValid(ex, headers, status, request);
    }

    @ExceptionHandler({AppException.class})
    public ResponseEntity<ApiResponse<Object>> handleAppException(HttpServletResponse response, AppException appException) {
        ApiResponse<Object> apiResponse = new ApiResponse<>(null, ResponseCode.ERROR, appException.getMessage());
        return new ResponseEntity<>(apiResponse, appException.getHttpStatus());
    }

    @ExceptionHandler({CsvValidationException.class})
    public ResponseEntity<ApiResponse<Object>> handleCsvException(HttpServletResponse response, CsvValidationException csvValidationException) {
        logger.info(csvValidationException.getMessage());
        ApiResponse<Object> apiResponse = new ApiResponse<>(null, ResponseCode.ERROR, "Unable to parse CSV");
        return new ResponseEntity<>(apiResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({IOException.class})
    public ResponseEntity<ApiResponse<Object>> handleCsvException(HttpServletResponse response, IOException csvValidationException) {
        logger.info(csvValidationException.getMessage());
        ApiResponse<Object> apiResponse = new ApiResponse<>(null, ResponseCode.ERROR, "Unable to parse the file");
        return new ResponseEntity<>(apiResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
