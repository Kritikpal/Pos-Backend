package com.kritik.POS.mobile.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuTaxMapDeletionDto {
    private Long menuItemId;
    private Long taxConfigId;
}