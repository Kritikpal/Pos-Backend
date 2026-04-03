package com.kritik.POS.events;

import com.kritik.POS.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class InventoryOrderEventListener {

    private final InventoryService inventoryService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("stockUpdateExecutor")
    public void handleOrderCompleted(OrderCompletedEvent event) {
        inventoryService.applyStockChangesForCompletedOrder(event.getOrderId());
    }
}
