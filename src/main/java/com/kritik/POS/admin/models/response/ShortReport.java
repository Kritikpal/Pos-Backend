package com.kritik.POS.admin.models.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class ShortReport {
    private Long totalOrderCount;
    private Long refundCount;
    private double lastOrderAmount;
    private double totalAmountEarned;
    private double averageOrderValue;
    private LocalDate reportDate;


}
