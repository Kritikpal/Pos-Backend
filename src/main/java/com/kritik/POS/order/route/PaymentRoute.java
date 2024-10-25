package com.kritik.POS.order.route;

public class PaymentRoute {
    public static final String INITIATE_PAYMENT = "/initiatePayment";
    public static final String CANCEL_PAYMENT = "/cancelPayment/{id}";
    public static final String COMPLETE_PAYMENT = "/completePayment/{id}";
    public static final String REFUND_PAYMENT = "/refundPayment/{id}";

}
