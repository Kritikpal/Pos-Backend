package com.kritik.POS.restaurant.util;

import com.kritik.POS.restaurant.DAO.ItemPrice;

public class RestaurantUtil {
    public static Double getMenuItemPrice(ItemPrice itemPrice) {
        Double price = itemPrice.getPrice();
        if (itemPrice.getDisCount() != null && itemPrice.getDisCount() != 0) {
            long round = Math.round(itemPrice.getPrice() - itemPrice.getPrice() * itemPrice.getDisCount() / 100);
            price = (double) round;
        }
        return price;
    }
}
