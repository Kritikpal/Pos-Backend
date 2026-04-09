package com.kritik.POS.order.model.response;

public interface PaymentByHourProjection {
    Integer getHourOfDay();
    Long getNumberOfPayments();
    Double getTotalRevenue();
}
