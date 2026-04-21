package com.kritik.POS.mobile.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PosPullRequest {

    @NotNull
    private Long restaurantId;

    @NotBlank
    private String deviceId;

    @Min(1)
    private Integer pageSize = 500;

    @NotNull
    private SyncTimeCursorBundle cursors;

    private List<String> requestedGroups;
}