package com.kritik.POS.admin.models.response;

import com.kritik.POS.order.DAO.Order;
import com.kritik.POS.order.DAO.SaleItem;
import com.kritik.POS.order.DAO.enums.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class LastOrderListItem {
    private String orderId;
    private Double totalPrice;
    private PaymentType paymentType;
    private String description;
    private LocalDateTime orderTime;
    public LastOrderListItem(Order order){
        this.totalPrice = order.getTotalPrice();
        this.paymentType = order.getPaymentType();
        this.orderId = order.getOrderId();
        List<SaleItem> orderItemList = order.getOrderItemList();
        StringBuilder description = new StringBuilder();
        for (SaleItem saleItem : orderItemList) {
            String saleItemName = saleItem.getSaleItemName();
            Integer amount = saleItem.getAmount();
            description.append(saleItemName).append(" * ").append(amount).append(" ");
        }
        this.orderTime = order.getLastUpdatedTime();
        this.description = description.toString().trim();
    }
}
