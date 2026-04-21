package com.kritik.POS.restaurant.util;

import com.kritik.POS.restaurant.entity.ItemPrice;
import com.kritik.POS.tax.util.MoneyUtils;
import java.math.BigDecimal;

public class RestaurantUtil {
    public static BigDecimal getMenuItemPrice(ItemPrice itemPrice) {
        BigDecimal price = MoneyUtils.money(itemPrice.getPrice());
        if (itemPrice.getDisCount() != null && itemPrice.getDisCount().compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal discountAmount = MoneyUtils.percentOf(price, itemPrice.getDisCount());
            price = MoneyUtils.subtract(price, discountAmount);
        }
        return price;
    }
}
