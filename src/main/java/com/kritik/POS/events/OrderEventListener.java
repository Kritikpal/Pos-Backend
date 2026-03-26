package com.kritik.POS.events;

import com.kritik.POS.invoice.service.InvoiceService;
import com.kritik.POS.order.entity.Order;
import com.kritik.POS.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final InvoiceService invoiceService;
    private final OrderRepository orderRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("invoiceExecutor")
    public void handleOrderCompleted(OrderCompletedEvent event) {


        Order order = orderRepository.findByIdWithItems(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        invoiceService.generateInvoice(order);
    }
}