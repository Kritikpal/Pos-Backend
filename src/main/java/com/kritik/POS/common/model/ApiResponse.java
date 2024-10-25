package com.kritik.POS.common.model;

import com.kritik.POS.common.enums.ResponseCode;
import com.kritik.POS.exception.errors.AppException;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

@Data
@Getter
@ToString
public class ApiResponse<T> {
    T data;
    ResponseCode responseCode;
    String message;

    public ApiResponse(T data, ResponseCode responseCode, String message) {
        this.data = data;
        this.responseCode = responseCode;
        this.message = message;
    }

    public static <T> ApiResponse<T> SUCCESS(T data) {
        return new ApiResponse<>(data, ResponseCode.SUCCESS, "Success");
    }

    public static <T> ApiResponse<T> ERROR(AppException appException) {
        return new ApiResponse<>(null, ResponseCode.ERROR, appException.getMessage());
    }

    public static <T> ApiResponse<T> SUCCESS(T data, String message) {
        return new ApiResponse<>(data, ResponseCode.SUCCESS, message);
    }

}
