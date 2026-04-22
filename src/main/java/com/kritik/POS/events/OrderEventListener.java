package com.kritik.POS.events;

import com.kritik.POS.invoice.service.InvoiceService;
import com.kritik.POS.order.api.OrderCompletedEvent;
import com.kritik.POS.order.api.OrderReadApi;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final InvoiceService invoiceService;
    private final OrderReadApi orderReadApi;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("invoiceExecutor")
    public void handleOrderCompleted(OrderCompletedEvent event) {
        invoiceService.generateInvoice(orderReadApi.getInvoiceSnapshot(event.orderId()));
    }
}
