package com.kritik.POS.order.api;

public interface OrderReadApi {

    OrderInvoiceSnapshot getInvoiceSnapshot(String orderId);
}
