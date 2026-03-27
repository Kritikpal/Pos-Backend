package com.kritik.POS.exception.advice;

import com.kritik.POS.common.enums.ResponseCode;
import com.kritik.POS.common.model.ApiResponse;
import com.kritik.POS.exception.errors.AppAuthenticationException;
import com.kritik.POS.exception.errors.AppException;
import com.opencsv.exceptions.CsvValidationException;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class AppControllerAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(AppAuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAppAuthenticationException(AppAuthenticationException exception) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ResponseCode.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Object>> handleAppException(AppException exception) {
        ResponseCode responseCode = exception.getHttpStatus() == HttpStatus.UNAUTHORIZED
                ? ResponseCode.UNAUTHORIZED
                : ResponseCode.ERROR;
        return buildResponse(exception.getHttpStatus(), responseCode, exception.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .collect(Collectors.joining(", "));
        return buildResponse(HttpStatus.BAD_REQUEST, ResponseCode.ERROR, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ResponseCode.ERROR,
                "Invalid value supplied for '" + exception.getName() + "'"
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingParameter(MissingServletRequestParameterException exception) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ResponseCode.ERROR,
                "Missing required parameter '" + exception.getParameterName() + "'"
        );
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingPart(MissingServletRequestPartException exception) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ResponseCode.ERROR,
                "Missing required request part '" + exception.getRequestPartName() + "'"
        );
    }

    @ExceptionHandler(CsvValidationException.class)
    public ResponseEntity<ApiResponse<Object>> handleCsvException(CsvValidationException exception) {
        logger.warn("Unable to parse CSV", exception);
        return buildResponse(HttpStatus.BAD_REQUEST, ResponseCode.ERROR, "Unable to parse CSV");
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiResponse<Object>> handleIoException(IOException exception) {
        logger.warn("File processing failed", exception);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ResponseCode.ERROR, "Unable to process the file");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException exception) {
        return buildResponse(HttpStatus.FORBIDDEN, ResponseCode.ERROR, "You do not have permission to access this resource");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnhandledException(Exception exception) {
        logger.error("Unhandled exception", exception);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ResponseCode.ERROR, "An unexpected error occurred");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<ObjectError> allErrors = ex.getBindingResult().getAllErrors();
        String message = allErrors.stream()
                .map(error -> error instanceof FieldError fieldError
                        ? fieldError.getField() + ": " + error.getDefaultMessage()
                        : error.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        return ResponseEntity.badRequest().body(new ApiResponse<>(null, ResponseCode.ERROR, message));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(null, ResponseCode.ERROR, "Malformed request body"));
    }

    private ResponseEntity<ApiResponse<Object>> buildResponse(HttpStatus status,
                                                              ResponseCode responseCode,
                                                              String message) {
        return new ResponseEntity<>(new ApiResponse<>(null, responseCode, message), status);
    }
}
