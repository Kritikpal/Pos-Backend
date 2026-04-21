package com.kritik.POS.order.route;

public final class ConfiguredOrderRoute {

    public static final String INITIATE = "/configured-orders/initiate";
    public static final String UPDATE = "/configured-orders/{id}";
    public static final String CANCEL = "/configured-orders/{id}/cancel";
    public static final String COMPLETE = "/configured-orders/{id}/complete";
    public static final String REFUND = "/configured-orders/{id}/refund";

    private ConfiguredOrderRoute() {
    }
}
