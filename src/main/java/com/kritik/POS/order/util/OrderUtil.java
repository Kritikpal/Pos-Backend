package com.kritik.POS.order.util;

import com.kritik.POS.order.DAO.Order;
import com.kritik.POS.order.DAO.OrderTax;
import com.kritik.POS.order.DAO.SaleItem;

public class OrderUtil {
    public static Double getTotalPrice(Order order) {
        double totalPrice = 0.0;
        if (order.getOrderItemList() != null && !order.getOrderItemList().isEmpty()) {
            for (SaleItem orderItem : order.getOrderItemList()) {
                totalPrice += orderItem.getSaleItemPrice() * orderItem.getAmount();
            }
            if (order.getOrderTaxes() != null && !order.getOrderTaxes().isEmpty()) {
                Double totaltax = 0.0;
                for (OrderTax orderTax : order.getOrderTaxes()) {
                    totaltax += orderTax.getTaxAmount();
                }
                if (totaltax != 0.0) {
                    totalPrice += (totalPrice * totaltax) / 100;
                }
            }
        }
        return totalPrice;
    }
}
