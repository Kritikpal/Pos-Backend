package com.kritik.POS.order.model.response;

import com.kritik.POS.order.entity.enums.PaymentType;

import java.time.LocalDateTime;

public interface LastOrderListItemProjection {

    String getOrderId();

    Double getTotalPrice();

    Integer getPaymentType();

    String getDescription();

    LocalDateTime getOrderTime();
}