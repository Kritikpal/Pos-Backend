package com.kritik.POS.mobile.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class PosBootstrapRequest {
    @jakarta.validation.constraints.NotNull
    private Long restaurantId;
    @jakarta.validation.constraints.NotBlank
    private String deviceId;
    private Long counterId;
    private String appVersion;
    private Integer schemaVersion;
    @jakarta.validation.constraints.Min(1)
    private Integer pageSize = 500;
    private List<String> requestedGroups;
}
