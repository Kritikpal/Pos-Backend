package com.kritik.POS.order.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentByHour {
    private Integer hourOfDay;
    private Long numberOfPayments;
}
