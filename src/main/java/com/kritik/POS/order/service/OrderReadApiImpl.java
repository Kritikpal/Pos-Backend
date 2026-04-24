package com.kritik.POS.order.service;

import com.kritik.POS.exception.errors.OrderException;
import com.kritik.POS.order.api.OrderInvoiceItemSnapshot;
import com.kritik.POS.order.api.OrderInvoiceSnapshot;
import com.kritik.POS.order.api.OrderInvoiceTaxSummarySnapshot;
import com.kritik.POS.order.api.OrderReadApi;
import com.kritik.POS.order.entity.ConfiguredSaleItem;
import com.kritik.POS.order.entity.Order;
import com.kritik.POS.order.entity.SaleItem;
import com.kritik.POS.order.repository.ConfiguredSaleItemRepository;
import com.kritik.POS.order.repository.OrderRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderReadApiImpl implements OrderReadApi {

    private final OrderRepository orderRepository;
    private final ConfiguredSaleItemRepository configuredSaleItemRepository;

    @Override
    @Transactional(readOnly = true)
    public OrderInvoiceSnapshot getInvoiceSnapshot(String orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(OrderException::new);

        List<OrderInvoiceItemSnapshot> items = new ArrayList<>();
        for (SaleItem item : order.getOrderItemList()) {
            items.add(new OrderInvoiceItemSnapshot(
                    item.getSaleItemName(),
                    item.getAmount(),
                    item.getSaleItemPrice(),
                    item.getLineTaxAmount(),
                    item.getLineTotalAmount()
            ));
        }
        for (ConfiguredSaleItem item : configuredSaleItemRepository.findAllByOrder_IdOrderByIdAsc(order.getId())) {
            String lineName = item.getLineName();
            if (!item.getSelections().isEmpty()) {
                String selectionSummary = item.getSelections().stream()
                        .map(selection -> selection.getSlotName() + ": " + selection.getChildItemName())
                        .reduce((left, right) -> left + ", " + right)
                        .orElse(null);
                if (selectionSummary != null && !selectionSummary.isBlank()) {
                    lineName = lineName + " (" + selectionSummary + ")";
                }
            }
            items.add(new OrderInvoiceItemSnapshot(
                    lineName,
                    item.getAmount(),
                    item.getUnitPrice(),
                    item.getLineTaxAmount(),
                    item.getLineTotalAmount()
            ));
        }

        return new OrderInvoiceSnapshot(
                order.getId(),
                order.getOrderId(),
                order.getSubtotalAmount(),
                order.getDiscountAmount(),
                order.getTaxableAmount(),
                order.getTaxAmount(),
                order.getFeeAmount(),
                order.getGrandTotal(),
                order.getOrderTaxContext() == null ? null : order.getOrderTaxContext().getSellerRegistrationNumberSnapshot(),
                order.getOrderTaxContext() == null ? null : order.getOrderTaxContext().getBuyerName(),
                order.getOrderTaxContext() == null ? null : order.getOrderTaxContext().getBuyerTaxId(),
                order.getOrderTaxContext() == null ? null : order.getOrderTaxContext().getBuyerTaxCategory(),
                order.getOrderTaxSummaries().stream()
                        .map(summary -> new OrderInvoiceTaxSummarySnapshot(
                                summary.getTaxDisplayName(),
                                summary.getTaxableBaseAmount(),
                                summary.getTaxAmount(),
                                summary.getCurrencyCode()
                        ))
                        .toList(),
                items
        );
    }
}
