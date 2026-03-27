package com.kritik.POS.restaurant.models.request;

import jakarta.validation.constraints.NotNull;

public record TableRequest(
        Long tableId,
        Long restaurantId,
        @NotNull(message = "Number of id is required") Integer noOfSeat,
        @NotNull(message = "Table number id is required") Integer tableNumber
) {

}
