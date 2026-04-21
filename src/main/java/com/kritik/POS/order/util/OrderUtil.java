package com.kritik.POS.order.util;

import com.kritik.POS.order.entity.Order;
import com.kritik.POS.order.entity.OrderTax;
import com.kritik.POS.order.entity.SaleItem;

import java.math.BigDecimal;

public class OrderUtil {
    public static BigDecimal getTotalPrice(Order order) {
        BigDecimal totalPrice = BigDecimal.ZERO;
        if (order.getOrderItemList() != null && !order.getOrderItemList().isEmpty()) {
            for (SaleItem orderItem : order.getOrderItemList()) {
                BigDecimal linePrice = orderItem.getSaleItemPrice() == null
                        ? BigDecimal.ZERO
                        : orderItem.getSaleItemPrice();
                totalPrice = totalPrice.add(linePrice.multiply(BigDecimal.valueOf(orderItem.getAmount())));
            }
            if (order.getOrderTaxes() != null && !order.getOrderTaxes().isEmpty()) {
                BigDecimal totalTaxRate = BigDecimal.ZERO;
                for (OrderTax orderTax : order.getOrderTaxes()) {
                    BigDecimal taxAmount = orderTax.getTaxAmount() == null
                            ? BigDecimal.ZERO
                            : BigDecimal.valueOf(orderTax.getTaxAmount());
                    totalTaxRate = totalTaxRate.add(taxAmount);
                }
                if (totalTaxRate.compareTo(BigDecimal.ZERO) != 0) {
                    totalPrice = totalPrice.add(
                            totalPrice.multiply(totalTaxRate).divide(BigDecimal.valueOf(100))
                    );
                }
            }
        }
        return totalPrice;
    }
}
