package com.kritik.POS.order.route;

public final class OrderV2Route {

    public static final String BASE = "/api/orders-v2";
    public static final String INITIATE = BASE + "/initiate";
    public static final String UPDATE = BASE + "/{id}";
    public static final String CANCEL = BASE + "/{id}/cancel";
    public static final String COMPLETE = BASE + "/{id}/complete";
    public static final String REFUND = BASE + "/{id}/refund";

    private OrderV2Route() {
    }
}
